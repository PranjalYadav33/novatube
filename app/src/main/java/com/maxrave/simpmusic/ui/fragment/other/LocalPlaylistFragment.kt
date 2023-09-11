package com.maxrave.simpmusic.ui.fragment.other

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build.ID
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.fragment.findNavController
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import coil.transform.Transformation
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.maxrave.simpmusic.R
import com.maxrave.simpmusic.adapter.playlist.PlaylistItemAdapter
import com.maxrave.simpmusic.common.Config
import com.maxrave.simpmusic.common.DownloadState
import com.maxrave.simpmusic.data.db.entities.SongEntity
import com.maxrave.simpmusic.data.model.browse.album.Track
import com.maxrave.simpmusic.data.queue.Queue
import com.maxrave.simpmusic.databinding.BottomSheetEditPlaylistTitleBinding
import com.maxrave.simpmusic.databinding.BottomSheetLocalPlaylistBinding
import com.maxrave.simpmusic.databinding.BottomSheetLocalPlaylistItemBinding
import com.maxrave.simpmusic.databinding.FragmentLocalPlaylistBinding
import com.maxrave.simpmusic.extension.toTrack
import com.maxrave.simpmusic.service.test.download.MusicDownloadService
import com.maxrave.simpmusic.viewModel.LocalPlaylistViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@AndroidEntryPoint
class LocalPlaylistFragment : Fragment() {
    private var _binding: FragmentLocalPlaylistBinding? = null
    private val binding get() = _binding!!

    private val viewModel by activityViewModels<LocalPlaylistViewModel>()

    lateinit var listTrack: ArrayList<Any>
    private lateinit var playlistAdapter: PlaylistItemAdapter

    private var id: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLocalPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK)
        {
            Log.d("ID", ID.toString())
            val intentRef = activityResult.data
            val data = intentRef?.data
            if (data != null)
            {
                val contentResolver = context?.contentResolver

                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                // Check for the freshest data.
                requireActivity().grantUriPermission(requireActivity().packageName, data, takeFlags)
                contentResolver?.takePersistableUriPermission(data, takeFlags)
                val uri = data.toString()
                viewModel.updatePlaylistThumbnail(uri, id!!)
                loadImage(uri)
            }
        }
    }

    @UnstableApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        id = arguments?.getLong("id")

        binding.loadingLayout.visibility = View.VISIBLE
        binding.rootLayout.visibility = View.GONE

        listTrack = arrayListOf()
        playlistAdapter = PlaylistItemAdapter(arrayListOf())

        binding.rvListSong.apply {
            adapter = playlistAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        if (id == null) {
            id = viewModel.id.value
            fetchDataFromDatabase()
            binding.loadingLayout.visibility = View.GONE
            binding.rootLayout.visibility = View.VISIBLE

        }
        else {
            fetchDataFromDatabase()
            binding.loadingLayout.visibility = View.GONE
            binding.rootLayout.visibility = View.VISIBLE
        }
        playlistAdapter.setOnClickListener(object : PlaylistItemAdapter.OnItemClickListener{
            override fun onItemClick(position: Int) {
                val args = Bundle()
                args.putString("type", Config.ALBUM_CLICK)
                args.putString("videoId", (listTrack[position] as SongEntity).videoId)
                args.putString("from", "Playlist \"${(viewModel.localPlaylist.value)?.title}\"")
                if (viewModel.localPlaylist.value?.downloadState == DownloadState.STATE_DOWNLOADED) {
                    args.putInt("downloaded", 1)
                }
                Queue.clear()
                Queue.setNowPlaying((listTrack[position] as SongEntity).toTrack())
                val tempList: ArrayList<Track> = arrayListOf()
                for (i in listTrack) {
                    tempList.add((i as SongEntity).toTrack())
                }
                Queue.addAll(tempList)
                if (Queue.getQueue().size >= 1) {
                    Queue.removeTrackWithIndex(position)
                }
                findNavController().navigate(R.id.action_global_nowPlayingFragment, args)
            }
        })
        playlistAdapter.setOnOptionClickListener(object : PlaylistItemAdapter.OnOptionClickListener{
            override fun onOptionClick(position: Int) {
                val dialog = BottomSheetDialog(requireContext())
                val viewDialog = BottomSheetLocalPlaylistItemBinding.inflate(layoutInflater)
                viewDialog.btDelete.setOnClickListener {
                    viewModel.deleteItem(viewModel.listTrack.value?.get(position), id!!)
                    viewModel.listTrack.observe(viewLifecycleOwner){
                        listTrack.clear()
                        listTrack.addAll(it)
                        playlistAdapter.updateList(listTrack)
                        dialog.dismiss()
                    }
                }
                dialog.setContentView(viewDialog.root)
                dialog.setCancelable(true)
                dialog.show()
            }
        })

        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        binding.topAppBarLayout.addOnOffsetChangedListener { it, verticalOffset ->
            Log.d("ArtistFragment", "Offset: $verticalOffset" + "Total: ${it.totalScrollRange}")
            if(abs(it.totalScrollRange) == abs(verticalOffset)) {
                binding.topAppBar.background = viewModel.gradientDrawable.value
                if (viewModel.gradientDrawable.value != null ){
                    if (viewModel.gradientDrawable.value?.colors != null){
                        requireActivity().window.statusBarColor = viewModel.gradientDrawable.value?.colors!!.first()
                    }
                }
            }
            else
            {
                binding.topAppBar.background = null
                requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark)
                Log.d("ArtistFragment", "Expanded")
            }
        }
        binding.btPlayPause.setOnClickListener {
                val args = Bundle()
                args.putString("type", Config.ALBUM_CLICK)
                args.putString("videoId", (listTrack[0] as SongEntity).videoId)
                args.putString("from", "Playlist \"${(viewModel.localPlaylist.value)?.title}\"")
                if (viewModel.localPlaylist.value?.downloadState == DownloadState.STATE_DOWNLOADED) {
                    args.putInt("downloaded", 1)
                }
                Queue.clear()
                Queue.setNowPlaying((listTrack[0] as SongEntity).toTrack())
                val tempList: ArrayList<Track> = arrayListOf()
                for (i in listTrack) {
                    tempList.add((i as SongEntity).toTrack())
                }
                Queue.addAll(tempList)
                if (Queue.getQueue().size >= 1) {
                    Queue.removeFirstTrackForPlaylistAndAlbum()
                }
                findNavController().navigate(R.id.action_global_nowPlayingFragment, args)
        }

        binding.btDownload.setOnClickListener {
            if (viewModel.localPlaylist.value?.downloadState == DownloadState.STATE_NOT_DOWNLOADED) {
                if (!viewModel.listTrack.value.isNullOrEmpty()) {
                    val listJob: ArrayList<SongEntity> = arrayListOf()
                    for (song in viewModel.listTrack.value!!){
                        if (song.downloadState == DownloadState.STATE_NOT_DOWNLOADED) {
                            listJob.add(song)
                        }
                    }
                    viewModel.listJob.value = listJob
                    Log.d("PlaylistFragment", "ListJob: ${viewModel.listJob.value}")
                    listJob.forEach {job ->
                        val downloadRequest =
                            DownloadRequest.Builder(job.videoId, job.videoId.toUri())
                                .setData(job.title.toByteArray())
                                .setCustomCacheKey(job.videoId)
                                .build()
                        viewModel.updateDownloadState(
                            job.videoId,
                            DownloadState.STATE_DOWNLOADING
                        )
                        DownloadService.sendAddDownload(
                            requireContext(),
                            MusicDownloadService::class.java,
                            downloadRequest,
                            false
                        )
                        viewModel.getDownloadStateFromService(job.videoId)
                    }
                    viewModel.downloadFullPlaylistState(id!!)
                }
            }
            else if (viewModel.localPlaylist.value?.downloadState == DownloadState.STATE_DOWNLOADED) {
                Toast.makeText(requireContext(), getString(R.string.downloaded), Toast.LENGTH_SHORT).show()
            }
            else if (viewModel.localPlaylist.value?.downloadState == DownloadState.STATE_DOWNLOADING) {
                Toast.makeText(requireContext(), getString(R.string.downloading), Toast.LENGTH_SHORT).show()
            }
        }
        binding.btMore.setOnClickListener {
            val moreDialog = BottomSheetDialog(requireContext())
            val moreDialogView = BottomSheetLocalPlaylistBinding.inflate(layoutInflater)
            moreDialogView.btEditTitle.setOnClickListener {
                val editDialog = BottomSheetDialog(requireContext())
                val editDialogView = BottomSheetEditPlaylistTitleBinding.inflate(layoutInflater)
                editDialogView.etPlaylistName.editText?.setText(viewModel.localPlaylist.value?.title)
                editDialogView.btEdit.setOnClickListener {
                    if (editDialogView.etPlaylistName.editText?.text.isNullOrEmpty()) {
                        Toast.makeText(requireContext(),
                            getString(R.string.playlist_name_cannot_be_empty), Toast.LENGTH_SHORT).show()
                    }
                    else {
                        viewModel.updatePlaylistTitle(editDialogView.etPlaylistName.editText?.text.toString(), id!!)
                        fetchDataFromDatabase()
                        editDialog.dismiss()
                        moreDialog.dismiss()
                    }
                }

                editDialog.setCancelable(true)
                editDialog.setContentView(editDialogView.root)
                editDialog.show()
            }
            moreDialogView.btDelete.setOnClickListener {
                Log.d("Check", "onViewCreated: ${viewModel.localPlaylist.value}")
                Log.d("Check", "onViewCreated: ${id}")
                viewModel.deletePlaylist(id!!)
                moreDialog.dismiss()
                Toast.makeText(requireContext(), "Playlist deleted", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }

            moreDialogView.btEditThumbnail.setOnClickListener {
                val intent = Intent()
                intent.type = "image/*"
                intent.action = Intent.ACTION_OPEN_DOCUMENT
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                resultLauncher.launch(intent)
            }

            moreDialog.setCancelable(true)
            moreDialog.setContentView(moreDialogView.root)
            moreDialog.show()
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    viewModel.playlistDownloadState.collectLatest { playlistDownloadState ->
                        when (playlistDownloadState) {
                            DownloadState.STATE_PREPARING -> {
                                binding.btDownload.visibility = View.GONE
                                binding.animationDownloading.visibility = View.VISIBLE
                            }

                            DownloadState.STATE_DOWNLOADING -> {
                                binding.btDownload.visibility = View.GONE
                                binding.animationDownloading.visibility = View.VISIBLE
                            }

                            DownloadState.STATE_DOWNLOADED -> {
                                binding.btDownload.visibility = View.VISIBLE
                                binding.animationDownloading.visibility = View.GONE
                                binding.btDownload.setImageResource(R.drawable.baseline_downloaded)
                            }

                            DownloadState.STATE_NOT_DOWNLOADED -> {
                                binding.btDownload.visibility = View.VISIBLE
                                binding.animationDownloading.visibility = View.GONE
                                binding.btDownload.setImageResource(R.drawable.download_button)
                            }
                        }
                    }
                }
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark)
        _binding = null
    }

    private fun fetchDataFromDatabase() {
        viewModel.clearLocalPlaylist()
        viewModel.id.postValue(id)
        viewModel.getLocalPlaylist(id!!)
        viewModel.localPlaylist.observe(viewLifecycleOwner) { localPlaylist ->
            Log.d("Check", "fetchData: ${viewModel.localPlaylist.value}")
            if (localPlaylist != null) {
                if (!localPlaylist.tracks.isNullOrEmpty()) {
                    viewModel.getListTrack(localPlaylist.tracks)
                    viewModel.listTrack.observe(viewLifecycleOwner) { list ->
                        Log.d("Check", "fetchData: ${viewModel.listTrack.value}")
                        listTrack.clear()
                        listTrack.addAll(list)
                        playlistAdapter.updateList(listTrack)
                    }
                }
            }
            if (localPlaylist != null) {
                binding.topAppBar.title = localPlaylist.title
            }
            binding.tvTrackCountAndTimeCreated.text = getString(R.string.album_length,
                localPlaylist?.tracks?.size?.toString() ?: "0", localPlaylist?.inLibrary?.format(
                DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy")
            ))
            loadImage(localPlaylist?.thumbnail)
            with(binding){
                if (localPlaylist != null) {
                    when(localPlaylist.downloadState) {
                        DownloadState.STATE_DOWNLOADED -> {
                            btDownload.visibility = View.VISIBLE
                            animationDownloading.visibility = View.GONE
                            btDownload.setImageResource(R.drawable.baseline_downloaded)
                        }

                        DownloadState.STATE_DOWNLOADING -> {
                            btDownload.visibility = View.GONE
                            animationDownloading.visibility = View.VISIBLE
                        }

                        DownloadState.STATE_PREPARING -> {
                            btDownload.visibility = View.GONE
                            animationDownloading.visibility = View.VISIBLE
                        }

                        DownloadState.STATE_NOT_DOWNLOADED -> {
                            btDownload.visibility = View.VISIBLE
                            animationDownloading.visibility = View.GONE
                            btDownload.setImageResource(R.drawable.download_button)
                        }
                    }
                }
            }
        }
    }

    private fun fetchDataFromViewModel() {
        Log.d("Check", "fetchDataFromViewModel: ${viewModel.localPlaylist.value}")
        val localPlaylist = viewModel.localPlaylist.value!!
        binding.topAppBar.title = localPlaylist.title
        binding.tvTrackCountAndTimeCreated.text = getString(R.string.album_length, localPlaylist.tracks?.size.toString(), localPlaylist.inLibrary.format(
            DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy")
        ))
        if (!viewModel.listTrack.value.isNullOrEmpty()) {
            listTrack.clear()
            listTrack.addAll(viewModel.listTrack.value!!)
            Log.d("Check", "fetchData: ${viewModel.listTrack.value}")
            playlistAdapter.updateList(listTrack)
        }
        loadImage(localPlaylist.thumbnail)
        with(binding){
            when(localPlaylist.downloadState) {
                DownloadState.STATE_DOWNLOADED -> {
                    btDownload.visibility = View.VISIBLE
                    animationDownloading.visibility = View.GONE
                    btDownload.setImageResource(R.drawable.baseline_downloaded)
                }
                DownloadState.STATE_DOWNLOADING -> {
                    btDownload.visibility = View.GONE
                    animationDownloading.visibility = View.VISIBLE
                }
                DownloadState.STATE_PREPARING -> {
                    btDownload.visibility = View.GONE
                    animationDownloading.visibility = View.VISIBLE
                }
                DownloadState.STATE_NOT_DOWNLOADED -> {
                    btDownload.visibility = View.VISIBLE
                    animationDownloading.visibility = View.GONE
                    btDownload.setImageResource(R.drawable.download_button)
                }
            }
        }
    }

    private fun loadImage(url: String?) {
        Log.d("Check", "loadImage: $url")
        val request = ImageRequest.Builder(requireContext())
            .data(url)
            .diskCachePolicy(CachePolicy.ENABLED)
            .diskCacheKey(id.toString())
            .placeholder(R.drawable.holder)
            .target(
                onStart = {
                    binding.ivPlaylistArt.setImageResource(R.drawable.holder)
                },
                onError = {
                    binding.ivPlaylistArt.setImageResource(R.drawable.holder)
                },
                onSuccess = { result ->
                    binding.ivPlaylistArt.setImageDrawable(result)
                    if (viewModel.gradientDrawable.value != null) {
                        viewModel.gradientDrawable.observe(viewLifecycleOwner) {
                            binding.fullRootLayout.background = it
                            binding.topAppBarLayout.background = ColorDrawable(it.colors?.get(0)!!)
                        }
                    }
                },
            )
            .transformations(object : Transformation{
                override val cacheKey: String
                    get() = id.toString()

                override suspend fun transform(input: Bitmap, size: Size): Bitmap {
                    val p = Palette.from(input).generate()
                    val defaultColor = 0x000000
                    var startColor = p.getDarkVibrantColor(defaultColor)
                    if (startColor == defaultColor){
                        startColor = p.getDarkMutedColor(defaultColor)
                        if (startColor == defaultColor){
                            startColor = p.getVibrantColor(defaultColor)
                            if (startColor == defaultColor){
                                startColor = p.getMutedColor(defaultColor)
                                if (startColor == defaultColor){
                                    startColor = p.getLightVibrantColor(defaultColor)
                                    if (startColor == defaultColor){
                                        startColor = p.getLightMutedColor(defaultColor)
                                    }
                                }
                            }
                        }
                        Log.d("Check Start Color", "transform: $startColor")
                    }
                    startColor = ColorUtils.setAlphaComponent(startColor, 150)
                    val endColor = resources.getColor(R.color.md_theme_dark_background, null)
                    val gd = GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        intArrayOf(startColor, endColor)
                    )
                    gd.cornerRadius = 0f
                    gd.gradientType = GradientDrawable.LINEAR_GRADIENT
                    gd.gradientRadius = 0.5f
                    viewModel.gradientDrawable.postValue(gd)
                    return input
                }

            }).build()
        ImageLoader(requireContext()).enqueue(request)
    }
}