import AVFoundation
import Foundation

// PTT 녹음 결과 — 업로드 포맷은 앱=audio/mp4(m4a/AAC) (contracts §힌트, Compose RecordedVoiceHint 미러)
struct RecordedVoiceHint {
    let audioData: Data

    let mimeType: String

    let fileName: String

    let durationMs: Int64
}

// AVAudioRecorder(m4a/AAC) 녹음 — 권한 없으면 start()가 요청만 트리거하고 false 반환(허용 후 다시 누름)
final class VoiceHintRecorder {
    private var recorder: AVAudioRecorder?

    private var outputUrl: URL?

    private var startedAt: Date?

    private func hasPermission() -> Bool {
        AVAudioSession.sharedInstance().recordPermission == .granted
    }

    private func releaseRecorder() {
        recorder = nil
        if let outputUrl {
            try? FileManager.default.removeItem(at: outputUrl)
        }
        outputUrl = nil
        startedAt = nil
    }

    func start() -> Bool {
        if !hasPermission() {
            AVAudioSession.sharedInstance().requestRecordPermission { _ in }
            return false
        }
        if recorder != nil {
            return false
        }
        let session = AVAudioSession.sharedInstance()
        let url = FileManager.default.temporaryDirectory
            .appendingPathComponent("ptt_hint_\(Int(Date().timeIntervalSince1970)).m4a")
        let settings: [String: Any] = [
            AVFormatIDKey: Int(kAudioFormatMPEG4AAC),
            AVSampleRateKey: 44_100,
            AVNumberOfChannelsKey: 1,
            AVEncoderBitRateKey: 64_000
        ]

        do {
            try session.setCategory(.playAndRecord, mode: .default, options: [.defaultToSpeaker])
            try session.setActive(true)

            let audioRecorder = try AVAudioRecorder(url: url, settings: settings)

            audioRecorder.record()
            recorder = audioRecorder
            outputUrl = url
            startedAt = Date()
            return true
        } catch {
            releaseRecorder()
            return false
        }
    }

    func stop() -> RecordedVoiceHint? {
        let durationMs = startedAt.map { Int64(Date().timeIntervalSince($0) * 1_000) } ?? 0

        guard let audioRecorder = recorder, let url = outputUrl else { return nil }
        audioRecorder.stop()
        defer { releaseRecorder() }
        // 너무 짧은 클립은 무효 — 화면이 "너무 짧아요" 안내 (Compose MIN_DURATION_MS 미러)
        if durationMs < 500 {
            return nil
        }
        guard let data = try? Data(contentsOf: url), !data.isEmpty else { return nil }

        return RecordedVoiceHint(
            audioData: data,
            mimeType: "audio/mp4",
            fileName: url.lastPathComponent,
            durationMs: durationMs
        )
    }

    func cancel() {
        recorder?.stop()
        releaseRecorder()
    }
}
