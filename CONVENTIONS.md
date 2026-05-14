# 백엔드 코딩 컨벤션

## 공통

### 변수 선언
- 변수 선언 시 항상 `val`을 사용한다. (본인 판단 하에 `var` 사용 가능하나 이에 따른 불이익은 사용자 본인에게 있다.)

### 의존성 주입 (DI)
클래스명 뒤에 작성하며 매개변수 취급한다.

```kotlin
// 의존성을 하나만 받는 경우
class CommunityLikeImpl(private val communityLikeRepository: CommunityLikeRepository):

// 의존성을 두 개 이상 받는 경우
class CommunityLikeImpl(private val communityLikeRepository: CommunityLikeRepository,
                        private val memberRepository: MemberRepository):
```

### orElseThrow 대체
Java의 `orElseThrow` 대신 `?:` 를 이용해 작성하며, `?:` 사용 전에 항상 줄바꿈한다.

```kotlin
// 예시 1
val member = memberRepository.findMemberById(communityLikeRequest.memberId)
            ?: throw ApplicationException(MemberStatusCode.MEMBER_NOT_FOUND)

// 예시 2
communityLikeRepository.delete(communityLikeRepository.findByMember(memberRepository.findMemberById(communityPostUnlikeRequest.memberId)
            ?: throw ApplicationException(MemberStatusCode.MEMBER_NOT_FOUND)
        )
            ?: throw ApplicationException(LikeStatusCode.NOT_LIKED)
        )
```

### 타입 선언
컴파일러에서 타입 추론이 가능한 경우 타입 선언을 생략한다.

```kotlin
// X - 타입 추론이 가능하지만 타입을 명시한 경우
val member: Member = memberRepository.findMemberById(communityLikeRequest.memberId)
            ?: throw ApplicationException(MemberStatusCode.MEMBER_NOT_FOUND)

// O - 타입 추론이 가능하므로 생략
val member = memberRepository.findMemberById(communityLikeRequest.memberId)
            ?: throw ApplicationException(MemberStatusCode.MEMBER_NOT_FOUND)
```

### 유스케이스 & 구현체
항상 유스케이스(인터페이스)를 만들고 한데 묶어 구현체(서비스)를 만든다.

### 상속 및 구현

```kotlin
// 두 개 이상 상속 받을 때
class CommunityLikeImpl(private val communityLikeRepository: CommunityLikeRepository,
                        private val memberRepository: MemberRepository):
    CommunityPostLikeUseCase,
    CommunityPostUnlikeUseCase {

// 하나만 상속 받을 때
class CommunityLikeImpl(private val communityLikeRepository: CommunityLikeRepository,
                        private val memberRepository: MemberRepository): CommunityPostLikeUseCase {
```

오버라이딩 시 `@Override` 어노테이션 대신 Kotlin 기본 문법을 사용한다.

```kotlin
override fun like(): CommunityPostLikeResponse {
```

### 파일 구조
```
service/
└── usecase/
│   ├── CommunityPostLikeUseCase
│   └── CommunityPostUnlikeUseCase
└── CommunityLikeImpl
```

### 객체 생성
Builder 패턴을 사용하지 않고 생성자를 이용해 생성한다.

---

## 엔티티 클래스

- 모든 엔티티 클래스의 ID는 우선적으로 `Long` 타입을 사용한다. (추후 UUID 등으로 변경될 수 있다.)
- ID는 `private`을 사용한다.
- ID의 setter는 만들지 않으며 getter는 아래와 같이 작성한다.

```kotlin
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private val id: Long? = null

fun getId() = id
```

---

## Java 공통 컨벤션

### 중괄호 및 줄바꿈

클래스 또는 메서드 시작 시 중괄호 이후 항상 한 줄 줄바꿈 후 코드 작성.  
모든 중괄호 이후에는 줄바꿈한다.

```java
@Service
@RequiredArgsConstructor
public class MemberUseCase {

    private final MemberRepository memberRepository;
```

파일 내 마지막 중괄호 닫은 이후 줄바꿈 금지.

```java
        return ApiResponse.ok("");
    }
}
```

### 람다식

```java
Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(
                        () -> new IllegalArgumentException("")
                );
```

### 공통 응답 반환
모든 리턴은 `BaseResponse`를 이용한 공통 응답 반환 및 `response` 활용.

```java
return BaseResponse.ok(SignUpResponse.of("가입되었습니다."));

public static SignUpResponse of(String message) {
    return new SignUpResponse(message);
}
```

### 소괄호 줄바꿈
소괄호 안에 후행 공백이 있는 경우 줄바꿈.

```java
GenerateTokenRequest generateTokenRequest = new GenerateTokenRequest(
        member.getNickname(),
        member.getRole()
);
```

### DTO 매개변수 풀네임 선언
DTO를 매개변수로 받을 때 DTO 풀네임으로 선언한다.

```java
public ApiResponse<SignInResponse> signIn(@Valid @RequestBody final SignInRequest signInRequest,
```

### 매개변수 순서
- **DTO가 스프링 기본 지원 타입(`HttpServletRequest`, `HttpServletResponse` 등)보다 앞에 위치해야 한다.**
- 매개변수가 2개 이상인 경우 두 번째 매개변수부터 줄바꿈한다.

```java
// O
public ApiResponse<SignInResponse> signIn(@Valid @RequestBody final SignInRequest signInRequest,
                                          HttpServletResponse httpServletResponse) {

// X
public BaseResponse<SignOutResponse> signOut(HttpServletRequest httpServletRequest,
                                             SignOutRequest signOutRequest) {
```

### final 사용
매개변수 바로 앞에 `final`을 사용한다.

### @Valid 어노테이션
컨트롤러에서 DTO 매개변수에 어노테이션이 있는 경우 `@Valid` 어노테이션 사용 필수.

```java
public ApiResponse<SignInResponse> signIn(@Valid @RequestBody final SignInRequest signInRequest,
```

### 기본 자료형 매개변수명
기본 자료형(`int`, `String` 등)을 사용하지 않는 모든 매개변수명은 해당 타입을 카멜케이스로 표기한다.

```java
public BaseResponse<SignOutResponse> signOut(HttpServletRequest httpServletRequest,
                                             HttpServletResponse httpServletResponse) {
```

### 어노테이션 순서
길이가 긴 어노테이션을 아래에 위치시킨다.

### 패키지명
`dto` 패키지 대신 `data` 패키지를 사용한다.

### Setter/Getter
- `@Setter` 어노테이션 사용 금지.
- `@Getter`는 사용 가능.

### return 문
`return` 문은 항상 줄바꿈 이후 사용한다.
