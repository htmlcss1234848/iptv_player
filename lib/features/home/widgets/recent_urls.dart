import 'package:flutter/material.dart';
import '../../../core/utils/url_parser.dart';
import '../../../core/models/stream_url_model.dart';

class RecentUrlsList extends StatelessWidget {
  final List<String> urls;
  final Function(String) onTap;
  final Function(String) onDelete;

  const RecentUrlsList({
    super.key,
    required this.urls,
    required this.onTap,
    required this.onDelete,
  });

  @override
  Widget build(BuildContext context) {
    if (urls.isEmpty) return const _EmptyState();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(vertical: 16),
          child: Row(
            children: [
              const Icon(Icons.history, color: Color(0xFF00E5FF), size: 16),
              const SizedBox(width: 8),
              Text(
                'RECENT',
                style: TextStyle(
                  color: const Color(0xFF00E5FF).withOpacity(0.8),
                  fontSize: 11,
                  letterSpacing: 2,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
        ),
        ...urls.map((url) => _UrlTile(
              url: url,
              onTap: () => onTap(url),
              onDelete: () => onDelete(url),
            )),
      ],
    );
  }
}

class _UrlTile extends StatelessWidget {
  final String url;
  final VoidCallback onTap;
  final VoidCallback onDelete;

  const _UrlTile({
    required this.url,
    required this.onTap,
    required this.onDelete,
  });

  @override
  Widget build(BuildContext context) {
    final stream = UrlParser.parse(url);
    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      decoration: BoxDecoration(
        color: const Color(0xFF111827),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: const Color(0xFF1F2937)),
      ),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(10),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
          child: Row(
            children: [
              _TypeBadge(type: stream.type),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  url,
                  style: const TextStyle(
                    color: Colors.white70,
                    fontSize: 12,
                    fontFamily: 'monospace',
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              IconButton(
                icon: const Icon(Icons.close, color: Colors.white24, size: 16),
                onPressed: onDelete,
                padding: EdgeInsets.zero,
                constraints: const BoxConstraints(
                  minWidth: 28,
                  minHeight: 28,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _TypeBadge extends StatelessWidget {
  final StreamType type;

  const _TypeBadge({required this.type});

  @override
  Widget build(BuildContext context) {
    final (label, color) = switch (type) {
      StreamType.hls => ('HLS', const Color(0xFF1DE9B6)),
      StreamType.ts => ('TS', const Color(0xFFFFAB40)),
      StreamType.xtream => ('XC', const Color(0xFF7C4DFF)),
      StreamType.xtreamHls => ('XC-HLS', const Color(0xFF7C4DFF)),
      StreamType.xtreamTs => ('XC-TS', const Color(0xFFFF4081)),
      StreamType.direct => ('HTTP', const Color(0xFF00E5FF)),
      StreamType.unknown => ('???', Colors.white30),
    };

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: color.withOpacity(0.15),
        borderRadius: BorderRadius.circular(4),
        border: Border.all(color: color.withOpacity(0.4)),
      ),
      child: Text(
        label,
        style: TextStyle(
          color: color,
          fontSize: 10,
          fontWeight: FontWeight.bold,
          letterSpacing: 1,
        ),
      ),
    );
  }
}

class _EmptyState extends StatelessWidget {
  const _EmptyState();

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 32),
      child: Center(
        child: Column(
          children: [
            Icon(
              Icons.tv_off,
              color: Colors.white.withOpacity(0.1),
              size: 48,
            ),
            const SizedBox(height: 12),
            Text(
              'No recent streams',
              style: TextStyle(
                color: Colors.white.withOpacity(0.2),
                fontSize: 14,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
