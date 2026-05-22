class DebugInfoModel {
  final String videoCodec;
  final String audioCodec;
  final int videoBitrate;      // bps
  final int audioBitrate;      // bps
  final int totalBitrate;      // bps
  final String resolution;
  final double fps;
  final int droppedFrames;
  final int bufferMs;          // buffered ahead in ms
  final String networkSpeed;   // human readable
  final String rendererName;
  final int videoWidth;
  final int videoHeight;

  const DebugInfoModel({
    this.videoCodec = '-',
    this.audioCodec = '-',
    this.videoBitrate = 0,
    this.audioBitrate = 0,
    this.totalBitrate = 0,
    this.resolution = '-',
    this.fps = 0.0,
    this.droppedFrames = 0,
    this.bufferMs = 0,
    this.networkSpeed = '-',
    this.rendererName = '-',
    this.videoWidth = 0,
    this.videoHeight = 0,
  });

  String get formattedVideoBitrate => _formatBitrate(videoBitrate);
  String get formattedAudioBitrate => _formatBitrate(audioBitrate);
  String get formattedTotalBitrate => _formatBitrate(totalBitrate);
  String get formattedBufferMs =>
      bufferMs > 1000 ? '${(bufferMs / 1000).toStringAsFixed(1)}s' : '${bufferMs}ms';

  static String _formatBitrate(int bps) {
    if (bps <= 0) return '-';
    if (bps >= 1000000) return '${(bps / 1000000).toStringAsFixed(2)} Mbps';
    if (bps >= 1000) return '${(bps / 1000).toStringAsFixed(1)} Kbps';
    return '$bps bps';
  }

  factory DebugInfoModel.fromMap(Map<dynamic, dynamic> map) {
    return DebugInfoModel(
      videoCodec: map['videoCodec'] as String? ?? '-',
      audioCodec: map['audioCodec'] as String? ?? '-',
      videoBitrate: (map['videoBitrate'] as int?) ?? 0,
      audioBitrate: (map['audioBitrate'] as int?) ?? 0,
      totalBitrate: (map['totalBitrate'] as int?) ?? 0,
      resolution: map['resolution'] as String? ?? '-',
      fps: (map['fps'] as num?)?.toDouble() ?? 0.0,
      droppedFrames: (map['droppedFrames'] as int?) ?? 0,
      bufferMs: (map['bufferMs'] as int?) ?? 0,
      networkSpeed: map['networkSpeed'] as String? ?? '-',
      rendererName: map['rendererName'] as String? ?? 'ExoPlayer',
      videoWidth: (map['videoWidth'] as int?) ?? 0,
      videoHeight: (map['videoHeight'] as int?) ?? 0,
    );
  }

  static DebugInfoModel empty() => const DebugInfoModel();
}
