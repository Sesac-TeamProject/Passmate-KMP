import AVFoundation
import SwiftUI
import Shared

enum VoiceHintPlaybackState {
    case idle
    case playing
    case paused
    case ended
    case failed
}

// 음성 힌트 클립 재생 제어 — Compose VoiceHintPlayerController 미러 (AVPlayer)
final class VoiceHintAudioPlayer: ObservableObject {
    @Published private(set) var state: VoiceHintPlaybackState = .idle

    @Published private(set) var positionMillis: Int64 = 0

    private var player: AVPlayer?

    private var timeObserver: Any?

    private var endObserver: NSObjectProtocol?

    private func clearObservers() {
        if let timeObserver, let player {
            player.removeTimeObserver(timeObserver)
        }
        if let endObserver {
            NotificationCenter.default.removeObserver(endObserver)
        }
        timeObserver = nil
        endObserver = nil
    }

    private func attachObservers(to player: AVPlayer, item: AVPlayerItem) {
        timeObserver = player.addPeriodicTimeObserver(
            forInterval: CMTime(seconds: 0.2, preferredTimescale: 600),
            queue: .main
        ) { [weak self] time in
            self?.positionMillis = Int64(time.seconds * 1000)
            if item.status == .failed {
                self?.state = .failed
            }
        }
        endObserver = NotificationCenter.default.addObserver(
            forName: .AVPlayerItemDidPlayToEndTime,
            object: item,
            queue: .main
        ) { [weak self] _ in
            self?.state = .ended
        }
    }

    func play(url: String) {
        if let clipUrl = URL(string: url) {
            let item = AVPlayerItem(url: clipUrl)
            let player = AVPlayer(playerItem: item)

            stop()
            self.player = player
            attachObservers(to: player, item: item)
            state = .playing
            positionMillis = 0
            player.play()
        } else {
            state = .failed
        }
    }

    func pause() {
        player?.pause()
        if state == .playing {
            state = .paused
        }
    }

    func resume() {
        if state == .ended || state == .failed {
            player?.seek(to: .zero)
            state = .playing
            player?.play()
        } else {
            state = .playing
            player?.play()
        }
    }

    func stop() {
        clearObservers()
        player?.pause()
        player = nil
        state = .idle
        positionMillis = 0
    }

    deinit {
        clearObservers()
    }
}

// PTT 학생 배너 — Figma "PTT 음성 힌트 상태 시트"(349:5051) 미러 (FR-040)
struct VoiceHintBannerView: View {
    let hint: VoiceHint

    @ObservedObject var player: VoiceHintAudioPlayer

    let onReplay: () -> Void

    var body: some View {
        if player.state == .playing || player.state == .paused {
            playingBar
        } else if player.state == .failed {
            failedChip
        } else {
            replayChip
        }
    }

    private var playingBar: some View {
        HStack(spacing: 12) {
            hintIcon
            VStack(alignment: .leading, spacing: 6) {
                Text("선생님 음성 힌트")
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.textPrimary)
                progressBar
            }
            Text("\(formatClock(player.positionMillis)) / \(formatClock(hint.durationMs))")
                .font(.system(size: 14, weight: .medium))
                .kerning(-0.28)
                .foregroundColor(PassmateColors.textSecondary)
            Button {
                if player.state == .playing {
                    player.pause()
                } else {
                    player.resume()
                }
            } label: {
                if player.state == .playing {
                    HStack(spacing: 3) {
                        RoundedRectangle(cornerRadius: 2)
                            .fill(PassmateColors.primary)
                            .frame(width: 4, height: 14)
                        RoundedRectangle(cornerRadius: 2)
                            .fill(PassmateColors.primary)
                            .frame(width: 4, height: 14)
                    }
                } else {
                    Text("▶")
                        .font(.system(size: 16))
                        .foregroundColor(PassmateColors.primary)
                }
            }
        }
        .padding(.leading, 14)
        .padding(.trailing, 16)
        .frame(height: 60)
        .frame(maxWidth: .infinity)
        .background(PassmateColors.surface)
        .overlay(RoundedRectangle(cornerRadius: 18).stroke(PassmateColors.border, lineWidth: 1))
        .cornerRadius(18)
    }

    private var hintIcon: some View {
        ZStack {
            Circle()
                .fill(PassmateColors.primary)
                .frame(width: 34, height: 34)
            VStack(spacing: 1) {
                RoundedRectangle(cornerRadius: 4)
                    .fill(PassmateColors.surface)
                    .frame(width: 8, height: 11)
                Rectangle()
                    .fill(PassmateColors.surface)
                    .frame(width: 2, height: 3)
            }
        }
    }

    private var progressBar: some View {
        let fraction = hint.durationMs > 0
            ? min(max(Double(player.positionMillis) / Double(hint.durationMs), 0), 1)
            : 0

        return ZStack(alignment: .leading) {
            RoundedRectangle(cornerRadius: 3)
                .fill(PassmateColors.fieldGray)
            RoundedRectangle(cornerRadius: 3)
                .fill(PassmateColors.primary)
                .frame(width: 140 * fraction)
        }
        .frame(width: 140, height: 6)
    }

    private var replayChip: some View {
        Button(action: onReplay) {
            HStack(spacing: 6) {
                Text("▶")
                    .font(.system(size: 12))
                    .foregroundColor(PassmateColors.primary)
                Text("다시 듣기 · \(formatClock(hint.durationMs))")
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.textPrimary)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .background(PassmateColors.surface)
            .overlay(RoundedRectangle(cornerRadius: 14).stroke(PassmateColors.border, lineWidth: 1))
            .cornerRadius(14)
        }
    }

    private var failedChip: some View {
        Button(action: onReplay) {
            HStack(spacing: 6) {
                Text("!")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(PassmateColors.chipOrangeText)
                Text("재생에 실패했어요 — 탭해서 다시 시도")
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.textPrimary)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .background(PassmateColors.surface)
            .overlay(RoundedRectangle(cornerRadius: 14).stroke(PassmateColors.border, lineWidth: 1))
            .cornerRadius(14)
        }
    }

    private func formatClock(_ millis: Int64) -> String {
        let totalSeconds = max(millis / 1000, 0)
        let minutes = totalSeconds / 60
        let seconds = totalSeconds % 60

        return String(format: "%02d:%02d", minutes, seconds)
    }
}

// MARK: - 프리뷰 (Figma 시안 비교용, 백엔드 불필요)

#Preview("재생 중") {
    let player = VoiceHintAudioPlayer()
    player.play(url: "https://cdn.passmate.app/hints/301.m4a")
    return VoiceHintBannerView(
        hint: VoiceHint(hintId: 301, questionNo: 3, clipUrl: "https://cdn.passmate.app/hints/301.m4a", durationMs: 42000),
        player: player,
        onReplay: {}
    )
    .padding()
}

#Preview("재생 완료 · 다시 듣기") {
    VoiceHintBannerView(
        hint: VoiceHint(hintId: 301, questionNo: 3, clipUrl: "https://cdn.passmate.app/hints/301.m4a", durationMs: 42000),
        player: VoiceHintAudioPlayer(),
        onReplay: {}
    )
    .padding()
}

#Preview("재생 실패") {
    let player = VoiceHintAudioPlayer()
    player.play(url: "")
    return VoiceHintBannerView(
        hint: VoiceHint(hintId: 301, questionNo: 3, clipUrl: "https://cdn.passmate.app/hints/301.m4a", durationMs: 42000),
        player: player,
        onReplay: {}
    )
    .padding()
}
