# ❤️ Shared Death Counter  - 운명 공동체 하드코어  플러그인

> **"모두의 목숨은 하나다."**  
> 플레이어들이 목숨을 공유하고, 전멸 시 세계가 **자동으로 초기화**되는 마인크래프트 서버 시스템입니다.

## 📖 프로젝트 소개

**Paper 플러그인**과 **배치 파일(.bat)** 이 상호작용하여 **자동화된 하드코어 로그라이크 서버**를 구축합니다.

게임 오버 시 관리자가 수동으로 맵을 지울 필요가 없습니다. 배치파일이 자동으로 세계를 붕괴시키고 새로운 시드에서 다음 '지구'를 시작합니다.

### 🛠 핵심 메커니즘
1.  **플러그인 (`SharedDeathCounter`)**: 게임 내 로직 담당. 목숨 공유, 사망 감지, 게임 오버 연출, 종료 신호 생성.
2.  **배치 파일 (`start.bat`)**: 서버 초기화 담당. 플러그인이 보낸 신호를 감지하여 월드 파일을 삭제하거나 유지하고 서버를 재시작.

---

## ✨ 주요 기능

*   **❤️ 목숨 공유 시스템**: 모든 플레이어가 하나의 목숨 풀을 공유합니다.
*   **🔄 자동 월드 리셋**: 목숨이 0이 되면 화려한 연출 후 서버가 종료되며, **맵 데이터가 자동으로 삭제**되고 재시작됩니다.
*   **📊 영구적인 통계**: 맵이 초기화되어도 플레이어의 **누적 사망 횟수, 플레이 타임 등은 유지**됩니다.
*   **🌍 시즌제**: 리셋될 때마다 '지구 번호'가 올라가며, 역사가 기록됩니다.
*   **🐲 엔딩 통계**: 엔더 드래곤 처치 시 승리 연출과 함께 통계가 출력됩니다.
*   **⚙️ 실시간 MOTD**: 서버 리스트에서 현재 지구 번호와 남은 목숨을 실시간으로 확인할 수 있습니다.

---

## 📥 설치 및 실행 방법

### 1. 요구 사항 (Prerequisites)
*   **OS**: Windows 10/11
*   **Java**: [JDK 21](https://www.oracle.com/java/technologies/downloads/) 이상 설치 필수

### 2. 설치 단계
1.  오른쪽 **Release**에서 서버 팩을 다운로드합니다.
2.  **서버 구동기(Paper) 다운로드**:
    *   [PaperMC 공식 다운로드 링크 (1.21.11)](https://papermc.io/downloads/paper)
    *   위 링크에서 최신 빌드를 다운로드하세요.
3.  다운로드한 파일(예: `paper-1.21.11-xx.jar`)을 압축 푼 폴더에 넣습니다.
4.  **[중요]** 넣은 파일 이름을 반드시 **`server.jar`** 로 변경하세요.
5.  `start.bat` 파일을 실행합니다.

### 3. 서버 메모리(RAM) 할당 수정
기본적으로 **4GB**로 설정되어 있습니다. 컴퓨터 사양에 맞춰 변경하려면:

1.  `start.bat` 파일을 우클릭 -> **[편집]** (또는 메모장으로 열기).
2.  아래 줄을 찾아 숫자를 변경합니다.
    ```batch
    java -Xms4G -Xmx4G -jar server.jar nogui
    ```
    *   예: 8GB로 변경 시 -> `-Xms8G -Xmx8G`

---

## 🎮 명령어 및 권한

모든 명령어는 `/sdc`로 시작하며, 기본적으로 **OP(관리자)** 권한이 필요합니다.

| 명령어 | 설명 | 권한 |
| :--- | :--- | :--- |
| `/sdc status` | 현재 남은 목숨, 지구 번호 등 상태 확인 | OP |
| `/sdc deaths` | 현재 접속한 플레이어들의 사망 횟수 조회 | 누구나 |
| `/sdc deaths [닉네임]` | 본인 또는 타인의 누적 사망 횟수 조회 | 누구나 |
| `/sdc setlife <숫자>` | 공유 목숨 개수 강제 설정 | OP |
| `/sdc addlife <숫자>` | 공유 목숨 추가 | OP |
| `/sdc reset` | 연출 없이 즉시 리셋 (게임 오버 처리) | OP |
| `/sdc hardreset` | **[주의]** 모든 데이터(통계 포함) 완전 초기화 | OP |

---

## ⚙️ 설정 파일 (config.yml)

`plugins/SharedDeathCounter/config.yml` 에서 세부 설정을 변경할 수 있습니다.

```yaml
default-lives: 3    # 리셋 후 시작할 기본 목숨

# 서버 MOTD 설정
# 플레이스홀더: {earth} 현재 지구 번호 {lives} 남은 목숨
server-settings:
  motd: "§e현재 지구: §f#{earth}   §c남은 목숨: §f{lives}"

display:    # 액션바(HUD) 표시 설정 (true/false)
  lives: true       # 공유 목숨 표시
  individual-deaths: true # 개인별 누적 사망 횟수 표시
  earth: true       # 지구(시즌) 번호 표시

# 게임 규칙 (하드코어 설정)
gamerules:
  naturalRegeneration: true  # 자연 회복
  showDeathMessages: true     # 사망 메시지 출력 여부

# 명령어
settings:
  allow-deaths-command: true # /sdc deaths 명령어 사용 가능 여부

# 메시지 커스텀
messages:
  kick-reason: "§4§l[{earth}번째 지구 붕괴]§c\n\n세계가 소멸했습니다.\n잠시 후 {earth_next}번째 지구가 생성됩니다."
  new-earth-title: "§b§l새 지구"
  new-earth-subtitle: "§f{earth}번째 지구에 진입했습니다."
```

## ⚠️ 주의사항

1.  **맵 삭제 경고**: 게임 오버 시 `world`, `world_nether`, `world_the_end` 폴더는 **영구적으로 삭제**됩니다. 
2.  **서버 아이콘**: 서버 폴더에 `server-icon.png` (64x64 픽셀) 파일을 넣으면 자동으로 적용됩니다.

---
