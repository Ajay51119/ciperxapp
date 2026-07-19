import 'dart:convert';
import 'dart:io';

import 'package:cross_file/cross_file.dart';
import 'package:file_picker/file_picker.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';
import 'package:share_plus/share_plus.dart';

import '../../models/cipher_key_file.dart';
import '../../services/cipher_codec.dart';
import '../../state/cipher_store.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key, required this.store});

  final CipherStore store;

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  final TextEditingController _input = TextEditingController();
  final TextEditingController _keyName = TextEditingController(text: 'MyKey');
  final Map<String, TextEditingController> _cipherInputs = {
    for (final letter in 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('')) letter: TextEditingController(),
  };
  int _tab = 0;
  bool _encodeMode = true;

  @override
  void dispose() {
    _input.dispose();
    _keyName.dispose();
    for (final controller in _cipherInputs.values) {
      controller.dispose();
    }
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final activeMap = widget.store.activeCipher;
    final output = _input.text.isEmpty
        ? ''
        : (_encodeMode ? CipherCodec.encode(_input.text, activeMap) : CipherCodec.decode(_input.text, activeMap));

    return Scaffold(
      appBar: AppBar(
        title: const Text('CipherApex'),
        actions: [
          IconButton(
            onPressed: widget.store.refreshPermissions,
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      body: IndexedStack(
        index: _tab,
        children: [
          _buildSetup(),
          _buildEncode(output),
          _buildKeys(),
          _buildAssist(),
        ],
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _tab,
        onDestinationSelected: (value) => setState(() => _tab = value),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.tune), label: 'Setup'),
          NavigationDestination(icon: Icon(Icons.lock), label: 'Encode'),
          NavigationDestination(icon: Icon(Icons.key), label: 'Keys'),
          NavigationDestination(icon: Icon(Icons.layers), label: 'Assist'),
        ],
      ),
    );
  }

  Widget _buildSetup() {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        _buildWelcomeHeader(),
        _Card(
          title: 'Create cipher',
          subtitle: 'Build a key that can be shared as a .cipherkey file.',
          child: Column(
            children: [
              TextField(controller: _keyName, decoration: const InputDecoration(labelText: 'Key name')),
              const SizedBox(height: 12),
              for (final letter in 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split(''))
                Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: TextField(
                    controller: _cipherInputs[letter],
                    decoration: InputDecoration(labelText: letter),
                  ),
                ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    child: FilledButton.tonal(
                      onPressed: () {
                        final random = CipherCodec.randomCipher();
                        for (final entry in random.entries) {
                          _cipherInputs[entry.key]?.text = entry.value;
                        }
                        if (mounted) setState(() {});
                      },
                      child: const Text('Generate Random Key'),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: FilledButton(
                      onPressed: () async {
                        final cipher = <String, String>{};
                        for (final entry in _cipherInputs.entries) {
                          if (entry.value.text.trim().isNotEmpty) {
                            cipher[entry.key] = entry.value.text.trim();
                          }
                        }
                        await widget.store.saveGeneratedKey(name: _keyName.text, cipher: cipher);
                        if (mounted) setState(() => _tab = 2);
                      },
                      child: const Text('Save Key'),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        _Card(
          title: 'Permissions',
          subtitle: 'Android-only overlay and accessibility controls. iOS keeps the shared cipher app experience.',
          child: Column(
            children: !kIsWeb && Platform.isAndroid
                ? [
                    SwitchListTile(
                      contentPadding: EdgeInsets.zero,
                      value: widget.store.overlayEnabled,
                      onChanged: (value) => widget.store.toggleOverlay(value),
                      title: const Text('Floating Assist'),
                      subtitle: Text(widget.store.overlayPermissionGranted ? 'Overlay ready' : 'Needs overlay permission'),
                    ),
                    ListTile(
                      contentPadding: EdgeInsets.zero,
                      title: const Text('Accessibility'),
                      subtitle: Text(widget.store.accessibilityEnabled ? 'Enabled' : 'Optional on Android'),
                      trailing: TextButton(
                        onPressed: widget.store.refreshPermissions,
                        child: const Text('Refresh'),
                      ),
                    ),
                  ]
                : const [
                    Text('iOS keeps the shared cipher app, key files, and encode/decode flow. Floating overlay is Android-only.'),
                  ],
          ),
        ),
      ],
    );
  }

  Widget _buildEncode(String output) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        SegmentedButton<bool>(
          segments: const [
            ButtonSegment(value: true, label: Text('Encode')),
            ButtonSegment(value: false, label: Text('Decode')),
          ],
          selected: {_encodeMode},
          onSelectionChanged: (value) => setState(() => _encodeMode = value.first),
        ),
        const SizedBox(height: 12),
        TextField(
          controller: _input,
          minLines: 5,
          maxLines: 8,
          onChanged: (_) => setState(() {}),
          decoration: const InputDecoration(
            labelText: 'Text',
            alignLabelWithHint: true,
            border: OutlineInputBorder(),
          ),
        ),
        const SizedBox(height: 12),
        _Card(
          title: 'Result',
          subtitle: widget.store.activeKey == null ? 'No active key yet' : 'Using ${widget.store.activeKey!.name}',
          child: SelectableText(output.isEmpty ? 'Output appears here.' : output),
        ),
      ],
    );
  }

  Widget _buildKeys() {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        _Card(
          title: 'Import .cipherkey',
          subtitle: 'Use the same file on Android and iOS.',
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              FilledButton.tonal(
                onPressed: () async {
                  final result = await FilePicker.platform.pickFiles(
                    type: FileType.custom,
                    allowedExtensions: ['cipherkey', 'json'],
                    withData: true,
                  );
                  final file = result?.files.single;
                  final bytes = file?.bytes;
                  if (file == null || bytes == null) return;
                  final text = String.fromCharCodes(bytes);
                  final key = CipherKeyFile.fromJson(Map<String, dynamic>.from(jsonDecode(text) as Map));
                  await widget.store.addOrReplaceKey(key);
                  if (mounted) setState(() => _tab = 1);
                },
                child: const Text('Import File'),
              ),
              const SizedBox(height: 12),
              if (widget.store.activeKey != null) ...[
                Text('Active key: ${widget.store.activeKey!.name}', style: Theme.of(context).textTheme.titleMedium),
                Text('${widget.store.activeKey!.cipher.length} mapped letters'),
              ] else
                const Text('No active key selected yet.'),
            ],
          ),
        ),
        const SizedBox(height: 16),
        for (final key in widget.store.savedKeys)
          Card(
            child: ListTile(
              title: Text(key.name),
              subtitle: Text('${key.cipher.length} letters - ${key.created.toLocal()}'),
              trailing: Wrap(
                spacing: 8,
                children: [
                  TextButton(
                    onPressed: () => widget.store.setActiveKey(key),
                    child: const Text('Use'),
                  ),
                  TextButton(
                    onPressed: () async {
                      final dir = await getTemporaryDirectory();
                      final file = File('${dir.path}/${key.name.replaceAll(' ', '_')}.cipherkey');
                      await file.writeAsString(key.toPrettyJson());
                      await Share.shareXFiles([XFile(file.path)]);
                    },
                    child: const Text('Share'),
                  ),
                ],
              ),
            ),
          ),
      ],
    );
  }

  Widget _buildAssist() {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        _Card(
          title: 'Floating Assist',
          subtitle: 'Android: hold the overlay to arm text detection. iOS keeps the shared cipher app only.',
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: !kIsWeb && Platform.isAndroid
                ? [
                    const Padding(
                      padding: EdgeInsets.only(bottom: 12),
                      child: Text('Hold the floating button, then hover or select text to get the CipherApex popup.'),
                    ),
                    Text(widget.store.overlayPermissionGranted ? 'Overlay permission granted' : 'Overlay permission needed'),
                    Text(widget.store.accessibilityEnabled ? 'Accessibility enabled' : 'Accessibility optional'),
                    const SizedBox(height: 12),
                    FilledButton(
                      onPressed: () => widget.store.toggleOverlay(!widget.store.overlayEnabled),
                      child: Text(widget.store.overlayEnabled ? 'Stop Overlay' : 'Start Overlay'),
                    ),
                  ]
                : const [
                    Text('Floating Assist is not available on iOS. The rest of the cipher app still works there.'),
                  ],
          ),
        ),
      ],
    );
  }
  Widget _buildWelcomeHeader() {
    return Container(
      margin: const EdgeInsets.only(bottom: 20),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(24),
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [
            const Color(0xFF1DB954).withOpacity(0.15),
            const Color(0xFF0F1115).withOpacity(0.0),
          ],
        ),
        border: Border.all(
          color: const Color(0xFF1DB954).withOpacity(0.3),
          width: 1.5,
        ),
      ),
      padding: const EdgeInsets.all(24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 60,
                height: 60,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  gradient: const LinearGradient(
                    colors: [Color(0xFF1DB954), Color(0xFF00E676)],
                  ),
                  boxShadow: [
                    BoxShadow(
                      color: const Color(0xFF1DB954).withOpacity(0.5),
                      blurRadius: 15,
                      offset: const Offset(0, 5),
                    ),
                  ],
                ),
                child: const Center(
                  child: Icon(
                    Icons.security,
                    color: Colors.black,
                    size: 32,
                  ),
                ),
              ),
              const SizedBox(width: 18),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    ShaderMask(
                      shaderCallback: (bounds) => const LinearGradient(
                        colors: [Color(0xFF1DB954), Color(0xFF00E676)],
                      ).createShader(bounds),
                      child: const Text(
                        'CipherApex',
                        style: TextStyle(
                          fontSize: 28,
                          fontWeight: FontWeight.bold,
                          color: Colors.white,
                          letterSpacing: 0.5,
                        ),
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      'Universal Decryption Suite',
                      style: TextStyle(
                        fontSize: 13,
                        color: Colors.grey.shade400,
                        fontWeight: FontWeight.w500,
                        letterSpacing: 0.5,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 20),
          Text(
            'Welcome to the unified CipherApex decrypted space. Create, share, and activate secret key sets. Run Floating Assist to decode cipher text live on your screen, or use the device context menu selection.',
            style: TextStyle(
              fontSize: 14,
              color: Colors.grey.shade300,
              height: 1.5,
            ),
          ),
        ],
      ),
    );
  }
}

class _Card extends StatelessWidget {
  const _Card({
    required this.title,
    required this.subtitle,
    required this.child,
  });

  final String title;
  final String subtitle;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(title, style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 4),
            Text(subtitle),
            const SizedBox(height: 16),
            child,
          ],
        ),
      ),
    );
  }
}
