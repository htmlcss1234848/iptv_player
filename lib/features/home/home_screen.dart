import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'home_controller.dart';
import 'widgets/url_input_field.dart';
import 'widgets/recent_urls.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final controller = Get.put(HomeController());

    return Scaffold(
      backgroundColor: const Color(0xFF0A0E1A),
      body: SafeArea(
        child: CustomScrollView(
          slivers: [
            SliverToBoxAdapter(child: _Header()),
            SliverPadding(
              padding: const EdgeInsets.symmetric(horizontal: 20),
              sliver: SliverList(
                delegate: SliverChildListDelegate([
                  const SizedBox(height: 8),
                  UrlInputField(
                    controller: controller.urlController,
                    onPlay: controller.onPlay,
                    onClear: controller.clearInput,
                  ),
                  const SizedBox(height: 8),
                  _StreamTypeLegend(),
                  const SizedBox(height: 8),
                  Obx(() => RecentUrlsList(
                        urls: controller.recentUrls,
                        onTap: controller.onRecentUrlTap,
                        onDelete: controller.onRecentUrlDelete,
                      )),
                  const SizedBox(height: 32),
                ]),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _Header extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(20, 24, 20, 20),
      child: Row(
        children: [
          Container(
            width: 40,
            height: 40,
            decoration: BoxDecoration(
              color: const Color(0xFF00E5FF).withOpacity(0.1),
              borderRadius: BorderRadius.circular(10),
              border: Border.all(
                color: const Color(0xFF00E5FF).withOpacity(0.3),
              ),
            ),
            child: const Icon(
              Icons.play_circle_outline,
              color: Color(0xFF00E5FF),
              size: 22,
            ),
          ),
          const SizedBox(width: 14),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                'IPTV PLAYER',
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  letterSpacing: 3,
                ),
              ),
              Text(
                'ExoPlayer • Native Android',
                style: TextStyle(
                  color: const Color(0xFF00E5FF).withOpacity(0.7),
                  fontSize: 11,
                  letterSpacing: 1,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _StreamTypeLegend extends StatelessWidget {
  final _types = const [
    ('HLS', Color(0xFF1DE9B6), '.m3u8'),
    ('TS', Color(0xFFFFAB40), '.ts'),
    ('XC', Color(0xFF7C4DFF), 'Xtream'),
    ('HTTP', Color(0xFF00E5FF), 'Direct'),
  ];

  @override
  Widget build(BuildContext context) {
    return Wrap(
      spacing: 8,
      runSpacing: 8,
      children: _types
          .map((t) => _LegendChip(label: t.$1, color: t.$2, desc: t.$3))
          .toList(),
    );
  }
}

class _LegendChip extends StatelessWidget {
  final String label;
  final Color color;
  final String desc;

  const _LegendChip({
    required this.label,
    required this.color,
    required this.desc,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      decoration: BoxDecoration(
        color: color.withOpacity(0.08),
        borderRadius: BorderRadius.circular(6),
        border: Border.all(color: color.withOpacity(0.25)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(
            label,
            style: TextStyle(
              color: color,
              fontSize: 10,
              fontWeight: FontWeight.bold,
              letterSpacing: 1,
            ),
          ),
          const SizedBox(width: 6),
          Text(
            desc,
            style: TextStyle(
              color: Colors.white.withOpacity(0.3),
              fontSize: 10,
            ),
          ),
        ],
      ),
    );
  }
}
