# 모두의 보드점수+ · Play 업로드 서명 키 설정

> 업로드 키 파일(*.jks), 비밀번호, Base64 문자열을 채팅 또는 GitHub 코드에 업로드하지 마세요. 공개 저장소에 커밋하면 안 됩니다.

## 1. 먼저 기존 앱 등록 여부 확인
Play Console에서 패키지 ID `io.github.workto0725.boardscoreplus`로 이미 출시하거나 테스트한 앱이 있는지 확인합니다. 있으면 해당 앱의 기존 업로드 키와 버전 정책을 우선 확인합니다. 새 키를 함부로 생성하지 마세요.

## 2. 처음 등록하는 신규 앱인 경우 (Windows PC)
Android Studio가 설치된 PC에서 JDK의 `keytool`을 사용합니다.

```powershell
keytool -genkeypair -v -keystore boardscoreplus-upload.jks -alias boardscoreplus-upload -keyalg RSA -keysize 3072 -validity 10000 -storetype PKCS12
```

암호를 설정하고, 생성된 `boardscoreplus-upload.jks`를 **별도 안전한 장소에 두 벌 이상 백업**합니다. 이메일이나 채팅으로 보내지 마세요. 업로드 키를 잃으면 이후 업데이트가 어려워집니다(Play App Signing의 업로드 키 재설정 절차가 적용될 수 있음).

## 3. GitHub Actions Secrets 등록
GitHub 저장소 → Settings → Secrets and variables → Actions → New repository secret 에서 다음 4개를 만듭니다.

| Secret | 값 |
| --- | --- |
| `BOARDSCORE_KEYSTORE_BASE64` | 아래 PowerShell 출력의 Base64 텍스트 |
| `BOARDSCORE_KEYSTORE_PASSWORD` | 키 저장소 비밀번호 |
| `BOARDSCORE_KEY_ALIAS` | `boardscoreplus-upload` |
| `BOARDSCORE_KEY_PASSWORD` | 해당 별칭의 개인키 비밀번호(동일하게 지정했다면 저장소 비밀번호) |

PowerShell에서 Base64 변환(생성한 파일 폴더):
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path ".\boardscoreplus-upload.jks")))
```

Base64 결과를 **GitHub Secret에만 붙여넣습니다.** 터미널 기록이나 클립보드 처리에 주의하세요.

## 4. AAB 빌드
GitHub → Actions → **BoardScore Plus signed Play AAB** → Run workflow.
빌드 성공 후 해당 실행의 Artifacts에서 `boardscoreplus-play-upload-aab`를 받아 Play Console에 업로드합니다. 현재 워크플로는 업로드 자체를 수행하지 않습니다.

## 5. 플레이 콘솔
새 앱 생성 → 앱 설정 및 정책 관련 정보 입력 → 테스트 트랙 → AAB 업로드.
Play App Signing 가입이 요구되면 해당 안내에 따라 등록합니다. 실제 개인정보 수집/처리 및 사용자 데이터 삭제 방법을 확정한 뒤 데이터 보안/개인정보 처리방침을 제출해야 합니다.

## 6. 중요한 출시 전 확인
- 로그인 없이 공동 게임방을 만들고 QR로 들어갈 수 있는지
- 여러 휴대폰에서 점수 동기화되는지
- PNG 저장과 사진 촬영/선택이 되는지
- 아이콘 실행은 홈, 진행 중 새로고침은 기존 게임 화면으로 복원되는지
- 앱에 표시되는 고지 및 사용자 데이터 삭제 절차가 실제 동작과 일치하는지

디버그 APK는 개발 테스트용이며 Play Console 제출용 서명된 AAB를 대신하지 않습니다.
