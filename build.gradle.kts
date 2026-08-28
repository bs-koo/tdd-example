// 모노레포 루트. 빌드 로직은 backend/build.gradle.kts 에 있다.
// 이 파일은 .claude/config.json 의 java-spring.detect 를 충족시키기 위해 존재한다
// (루트에서 ./gradlew test 가 :backend:test 로 위임된다).
