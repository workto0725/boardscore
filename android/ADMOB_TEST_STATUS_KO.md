# 모두의 보드점수+ 광고 수익화 구현 상태

## 현재 구현
- Google Mobile Ads SDK (Android) 의존성을 추가했습니다.
- Android **debug 빌드에서만** Google의 공식 데모 앱 ID 및 테스트 배너 ID를 사용합니다.
- 앱 홈의 게임 선택 화면에만 배너를 표시하고, 점수 입력/게임 진행 중에는 숨깁니다.
- release 빌드는 `FLAG_DEBUGGABLE` 검사로 광고 배너를 생성하지 않습니다.
- 따라서 현재 **광고 수익은 발생하지 않습니다.**
- Google 제공 데모 ID는 사용자 AdMob 계정 ID가 아닙니다.

## 정식 광고 출시 전에 필요한 작업
1. AdMob 계정 및 실제 앱 연결, 본인 명의 앱 ID/광고 단위 ID 발급.
2. 개인정보 처리방침, 실제 서버 기록/사진 처리, 데이터 삭제 절차 확정.
3. Google UMP SDK를 이용한 사용자 동의와 해당 지역의 광고/개인정보 정책 대응.
4. 앱 심사 및 광고 관련 데이터 보안 양식 검토.
5. 디버그 테스트, 실기기 테스트 뒤 **실제 ID는 승인 후 별도 적용**.
6. 게임 종료 광고는 테스트·사용성 검증을 마친 뒤 별도 개발. 현재는 없음.
7. 광고 제거 인앱결제는 Play Billing과 구매 검증 서버를 설계한 뒤 도입. 현재는 없음.

## 확인할 사항
- GitHub Actions > BoardScore Plus Android build의 최신 빌드 성공.
- debug APK 설치 후 홈에 'Test Ad' 표시.
- 게임 선택 후 광고 컨테이너가 숨겨지고 점수 입력 영역에 겹치지 않는지.
- QR 공동 게임방과 재접속/사진 공유가 변함없이 작동하는지.
- 서명된 release AAB는 현재 광고 없이 동작해야 함.

관련 공식 문서:
- https://developers.google.com/admob/android/quick-start
- https://developers.google.com/admob/android/banner
- https://developers.google.com/admob/android/test-ads
