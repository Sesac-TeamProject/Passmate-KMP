import SwiftUI
import Shared

// Figma "UI 디자인 v6" M-13 새 방 만들기 시트(406:5893) 미러 — 방 이름·문제 세트·방 유형 → PIN 발급.
// 시트 표시 여부는 호스팅 화면(HostedRoomsView)이 소유한다 (규칙 §11-1)
struct CreateRoomSheetView: View {
    var onCreated: (String) -> Void = { _ in }

    var onNotice: (String) -> Void = { _ in }

    var onClose: () -> Void = {}

    @StateObject private var viewModel = CreateRoomViewModel(
        getMyQuestionSetsUseCase: KoinHelper.shared.getMyQuestionSetsUseCase(),
        createRoomUseCase: KoinHelper.shared.createRoomUseCase()
    )

    var body: some View {
        CreateRoomContentView(
            uiState: viewModel.uiState,
            onAction: { viewModel.action($0) },
            onClose: onClose
        )
        .onAppear {
            viewModel.action(.enter)
        }
        .onReceive(viewModel.event) { event in
            switch event {
            case let .created(pin):
                onCreated(pin)
            case let .showNotice(message):
                onNotice(message)
            }
        }
    }
}

private struct CreateRoomContentView: View {
    let uiState: CreateRoomUiState

    let onAction: (CreateRoomAction) -> Void

    let onClose: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                HStack {
                    Text("새 방 만들기")
                        .font(.system(size: 20, weight: .bold))
                        .kerning(-0.4)
                        .foregroundColor(PassmateColors.textPrimary)
                    Spacer()
                    Button(action: onClose) {
                        Text("✕")
                            .font(.system(size: 18))
                            .foregroundColor(PassmateColors.textSecondary)
                    }
                }
                fieldLabel("방 이름")
                TextField("예: 8월 4주차 Spring 스터디", text: Binding(
                    get: { uiState.title },
                    set: { onAction(.changeTitle(title: $0)) }
                ))
                .font(.system(size: 14))
                .padding(16)
                .background(PassmateColors.fieldGray)
                .cornerRadius(14)
                fieldLabel("문제 세트")
                setSelector
                fieldLabel("방 유형")
                paidToggle
                if uiState.isPaid {
                    fieldLabel("참가비 (1 C = ₩1)")
                    TextField("예: 10000", text: Binding(
                        get: { uiState.entryFeeText },
                        set: { onAction(.changeEntryFee(text: $0)) }
                    ))
                    .font(.system(size: 14))
                    .keyboardType(.numberPad)
                    .padding(16)
                    .background(PassmateColors.fieldGray)
                    .cornerRadius(14)
                }
                Text("PIN은 방을 만들면 자동 발급 · 프로젝터 화면은 웹에서")
                    .font(.system(size: 12))
                    .kerning(-0.24)
                    .foregroundColor(PassmateColors.textTertiary)
                    .frame(maxWidth: .infinity)
                submitButton
            }
            .padding(.horizontal, 20)
            .padding(.top, 24)
            .padding(.bottom, 28)
        }
        .background(PassmateColors.surface.ignoresSafeArea())
    }

    private func fieldLabel(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 13, weight: .medium))
            .kerning(-0.26)
            .foregroundColor(PassmateColors.textSecondary)
    }

    @ViewBuilder
    private var setSelector: some View {
        if uiState.isLoadingSets {
            HStack {
                Spacer()
                ProgressView().tint(PassmateColors.primary)
                Spacer()
            }
            .frame(height: 52)
            .background(PassmateColors.fieldGray)
            .cornerRadius(14)
        } else if uiState.setsLoadFailed {
            Button(action: { onAction(.retrySets) }) {
                Text("세트를 불러오지 못했어요 · 다시 시도")
                    .font(.system(size: 14))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.weakTopicText)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(16)
                    .background(PassmateColors.fieldGray)
                    .cornerRadius(14)
            }
        } else if uiState.sets.isEmpty {
            Text("확정된 문제 세트가 없어요 · 웹에서 세트를 만들고 확정해 주세요")
                .font(.system(size: 13))
                .kerning(-0.26)
                .foregroundColor(PassmateColors.textSecondary)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(16)
                .background(PassmateColors.fieldGray)
                .cornerRadius(14)
        } else {
            Menu {
                ForEach(uiState.sets, id: \.setId) { set in
                    Button(setLabel(set)) {
                        onAction(.selectSet(setId: set.setId))
                    }
                }
            } label: {
                HStack {
                    Text(uiState.selectedSet.map { setLabel($0) } ?? "문제 세트 선택")
                        .font(.system(size: 14, weight: .medium))
                        .kerning(-0.28)
                        .foregroundColor(PassmateColors.textPrimary)
                    Spacer()
                    Text("▾")
                        .font(.system(size: 14))
                        .foregroundColor(PassmateColors.textSecondary)
                }
                .padding(16)
                .background(PassmateColors.fieldGray)
                .cornerRadius(14)
            }
        }
    }

    private var paidToggle: some View {
        HStack(spacing: 4) {
            paidOption(label: "무료", isSelected: !uiState.isPaid, isPaidValue: false)
            paidOption(label: "유료 (Lv.3부터)", isSelected: uiState.isPaid, isPaidValue: true)
        }
        .padding(4)
        .background(PassmateColors.fieldGray)
        .cornerRadius(14)
    }

    private func paidOption(label: String, isSelected: Bool, isPaidValue: Bool) -> some View {
        Button(action: { onAction(.selectPaid(isPaid: isPaidValue)) }) {
            Text(label)
                .font(.system(size: 14, weight: .medium))
                .kerning(-0.28)
                .foregroundColor(isSelected ? PassmateColors.primaryDeep : PassmateColors.textSecondary)
                .frame(maxWidth: .infinity)
                .frame(height: 44)
                .background(isSelected ? PassmateColors.surface : PassmateColors.fieldGray)
                .cornerRadius(12)
        }
    }

    private var submitButton: some View {
        Button(action: { onAction(.submit) }) {
            Group {
                if uiState.isSubmitting {
                    ProgressView().tint(PassmateColors.surface)
                } else {
                    Text("방 만들기 → PIN 발급")
                        .font(.system(size: 15, weight: .bold))
                        .kerning(-0.3)
                        .foregroundColor(PassmateColors.surface)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .background(uiState.canSubmit ? PassmateColors.primary : PassmateColors.border)
            .cornerRadius(16)
        }
        .disabled(!uiState.canSubmit)
    }

    private func setLabel(_ set: QuestionSetSummary) -> String {
        "\(set.title) (\(set.questionCount)문항)"
    }
}
