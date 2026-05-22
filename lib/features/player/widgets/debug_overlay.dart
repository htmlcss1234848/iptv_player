import 'package:flutter/material.dart';
import '../../../core/models/debug_info_model.dart';

class DebugOverlay extends StatelessWidget {
  final DebugInfoModel info;

  const DebugOverlay({super.key, required this.info});

  @override
  Widget build(BuildContext context) {
    return Align(
      alignment: Alignment.topLeft,
      child: Container(
        margin: const EdgeInsets.all(12),
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: Colors.black.withOpacity(0.75),
          borderRadius: BorderRadius.circular(8),
          border: Border.all(
            color: const Color(0xFF00E5FF).withOpacity(0.3),
          ),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            _DebugHeader(),
            const SizedBox(height: 8),
            _DebugRow('Renderer', info.rendererName,
                color: const Color(0xFF00E5FF)),
            _DebugRow('Resolution',
                '${info.videoWidth}×${info.videoHeight}  ${info.fps.toStringAsFixed(1)} fps'),
            _DebugRow('Video', '${info.videoCodec}  ${info.formattedVideoBitrate}'),
            _DebugRow('Audio', '${info.audioCodec}  ${info.formattedAudioBitrate}'),
            _DebugRow('Total Bitrate', info.formattedTotalBitrate,
                color: const Color(0xFF1DE9B6)),
            _DebugRow('Buffer', info.formattedBufferMs),
            _DebugRow('Network', info.networkSpeed),
            _DebugRow('Dropped Frames', '${info.droppedFrames}',
                color: info.droppedFrames > 10
                    ? const Color(0xFFFF5252)
                    : Colors.white70),
          ],
        ),
      ),
    );
  }
}

class _DebugHeader extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Container(
          width: 8,
          height: 8,
          decoration: const BoxDecoration(
            color: Color(0xFF00E5FF),
            shape: BoxShape.circle,
          ),
        ),
        const SizedBox(width: 6),
        const Text(
          'DEBUG INFO',
          style: TextStyle(
            color: Color(0xFF00E5FF),
            fontSize: 10,
            fontWeight: FontWeight.bold,
            letterSpacing: 2,
          ),
        ),
      ],
    );
  }
}

class _DebugRow extends StatelessWidget {
  final String label;
  final String value;
  final Color color;

  const _DebugRow(
    this.label,
    this.value, {
    this.color = Colors.white70,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(
        children: [
          SizedBox(
            width: 110,
            child: Text(
              label,
              style: TextStyle(
                color: Colors.white.withOpacity(0.4),
                fontSize: 11,
                fontFamily: 'monospace',
              ),
            ),
          ),
          Text(
            value,
            style: TextStyle(
              color: color,
              fontSize: 11,
              fontFamily: 'monospace',
              fontWeight: FontWeight.w500,
            ),
          ),
        ],
      ),
    );
  }
}
