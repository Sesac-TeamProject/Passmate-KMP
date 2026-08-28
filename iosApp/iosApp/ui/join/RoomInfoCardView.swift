import SwiftUI
import Shared

// T081(US11)/T086(US12): 입장 전 방 정보 — 제목·호스트 등급 뱃지·평균 별점·평가 수 (M-01 입장 전).
// JoinView 삽입 슬롯에 붙는다 (분담 접점 ①)
struct RoomInfoCardView: View {
    let room: RoomInfo

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(room.title)
                .font(.system(size: 16, weight: .bold))
                .kerning(-0.32)
                .foregroundColor(PassmateColors.textPrimary)
            if let host = room.host {
                HStack(spacing: 6) {
                    Text("\(host.nickname) 선생님")
                        .font(.system(size: 14, weight: .medium))
                        .kerning(-0.28)
                        .foregroundColor(PassmateColors.textSecondary)
                    if let level = HostLevel.from(host.level?.intValue) {
                        ReputationBadgeView(level: level)
                    }
                }
                ratingRow(avgStars: host.avgStars?.doubleValue, ratingCount: host.ratingCount?.intValue)
            }
            metaRow
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(PassmateColors.surface)
        .overlay(RoundedRectangle(cornerRadius: 16).stroke(PassmateColors.border, lineWidth: 1))
        .cornerRadius(16)
    }

    @ViewBuilder
    private func ratingRow(avgStars: Double?, ratingCount: Int?) -> some View {
        if let avgStars, (ratingCount ?? 0) > 0 {
            HStack(spacing: 4) {
                Text("★")
                    .font(.system(size: 14))
                    .foregroundColor(PassmateColors.starGold)
                Text("\(formatStars(avgStars)) · 평가 \(ratingCount ?? 0)개")
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.textPrimary)
            }
        } else {
            Text("아직 평가가 없어요")
                .font(.system(size: 13))
                .kerning(-0.26)
                .foregroundColor(PassmateColors.textTertiary)
        }
    }

    private var metaRow: some View {
        var parts: [String] = []

        if let count = room.questionCount { parts.append("\(count)문항") }
        if let mins = room.estimatedMinutes { parts.append("약 \(mins)분") }
        if room.isPaid, let fee = room.entryFee { parts.append("참가비 \(fee)코인") }

        return Group {
            if !parts.isEmpty {
                Text(parts.joined(separator: " · "))
                    .font(.system(size: 12))
                    .kerning(-0.24)
                    .foregroundColor(PassmateColors.textSecondary)
            }
        }
    }

    private func formatStars(_ avgStars: Double) -> String {
        let rounded = Int((avgStars * 10).rounded())

        return "\(rounded / 10).\(rounded % 10)"
    }
}
