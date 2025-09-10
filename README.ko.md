[English](README.md) | 한국어

# 멜론 소스 플러그인

이 프로젝트는 [Lavalink](https://github.com/lavalink-devs/Lavalink)용 플러그인으로, [멜론](https://www.melon.com/)의 트랙 재생을 지원합니다.

## 기능
- `mplay:` 접두사를 통해 가장 관련성 높은 YouTube 결과 재생
- 멜론 곡 URL을 직접 로드

## 빌드

이 플러그인은 **Lavalink 4.x**를 대상으로 하며 **Java 17** 이상이 필요합니다.

```bash
./gradlew shadowJar
```

생성된 셰이드 JAR는 `build/libs`에 위치합니다.

## 사용법

생성된 JAR를 Lavalink의 `plugins` 디렉터리에 복사하고 Lavalink 설정에서 플러그인을 활성화하세요.

## 설정

`application.yml`을 통해 플러그인을 활성화하거나 비활성화할 수 있습니다:

```yaml
plugins:
  melon:
    enabled: true
```

## 라이선스

MIT
