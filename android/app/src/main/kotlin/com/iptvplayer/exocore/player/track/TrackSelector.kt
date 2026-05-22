package com.iptvplayer.exocore.player.track

import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer

data class TrackInfo(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val type: String,        // "video" | "audio" | "subtitle"
    val isSelected: Boolean,
    val bitrate: Int = 0,
    val resolution: String = "",
    val language: String = "",
)

class TrackSelector(private val player: ExoPlayer) {

    fun getAvailableTracks(): List<TrackInfo> {
        val tracks = player.currentTracks
        val result = mutableListOf<TrackInfo>()

        for (groupIndex in 0 until tracks.groups.size) {
            val group = tracks.groups[groupIndex]
            val type = when (group.type) {
                C.TRACK_TYPE_VIDEO    -> "video"
                C.TRACK_TYPE_AUDIO    -> "audio"
                C.TRACK_TYPE_TEXT     -> "subtitle"
                else                  -> continue
            }

            for (trackIndex in 0 until group.length) {
                val format = group.getTrackFormat(trackIndex)
                val selected = group.isTrackSelected(trackIndex)

                val label = buildLabel(type, format, trackIndex)
                result.add(
                    TrackInfo(
                        groupIndex = groupIndex,
                        trackIndex = trackIndex,
                        label = label,
                        type = type,
                        isSelected = selected,
                        bitrate = format.bitrate.coerceAtLeast(0),
                        resolution = if (format.width > 0) "${format.width}x${format.height}" else "",
                        language = format.language ?: "",
                    )
                )
            }
        }
        return result
    }

    fun selectTrack(groupIndex: Int, trackIndex: Int) {
        val tracks = player.currentTracks
        if (groupIndex >= tracks.groups.size) return
        val group = tracks.groups[groupIndex]

        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .addOverride(
                TrackSelectionOverride(group.mediaTrackGroup, trackIndex)
            )
            .build()
    }

    fun resetToAuto() {
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .clearOverrides()
            .build()
    }

    private fun buildLabel(type: String, format: androidx.media3.common.Format, index: Int): String {
        return when (type) {
            "video" -> {
                val res = if (format.height > 0) "${format.height}p" else "Video ${index + 1}"
                val bitrate = if (format.bitrate > 0) " ${format.bitrate / 1000}K" else ""
                "$res$bitrate"
            }
            "audio" -> {
                val lang = format.language?.uppercase() ?: "Audio ${index + 1}"
                val ch = if (format.channelCount > 0) " ${format.channelCount}ch" else ""
                "$lang$ch"
            }
            "subtitle" -> format.language?.uppercase() ?: "Sub ${index + 1}"
            else -> "Track ${index + 1}"
        }
    }
}
