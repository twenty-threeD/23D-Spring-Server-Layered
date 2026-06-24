아래는 현재 프로젝트의 코딩 컨벤션입니다. 이를 검토한 후, 현재 열려 있는 파일에서 이 컨벤션을 위반하는 코드를 확인하고 결과를 보고하세요.

## 공통

여는 중괄호 뒤에는 항상 줄바꿈을 한 후 코드를 작성한다.

```kotlin
@Service
@Transactional(rollbackFor = [Exception::class])
class MemberServiceImpl: MemberService {

    override fun deleteAccount
```

파일 최하단 코드의 닫는 중괄호 뒤에는 빈 줄을 두지 않는다.
```kotlin
		return ApiResponse.ok("");
}
}
```

공통 응답은 반드시 `BaseResponse`를 사용하며, `response`를 활용한다.
```kotlin
return BaseResponse.ok(SignUpResponse.of("가입되었습니다."));
```

괄호 안에 값이 2개 이상인 경우, 각각 줄바꿈하여 작성한다.
```kotlin
return SignInResponse.of(
            tokenService.generateAccessToken(
                generateTokenRequest,
                httpServletResponse
            ),
            tokenService.generateRefreshToken(
                generateTokenRequest,
                httpServletResponse
            )
        )
```

파라미터 이름은 클래스 이름을 camelCase로 작성한다.

어노테이션은 짧은 것을 위에, 긴 것을 아래에 배치한다.

`return` 문 앞에는 항상 빈 줄을 넣는다.

파라미터 목록에서 DTO는 Spring Boot 기본 타입(예: `HttpServletRequest`)보다 앞에 위치시킨다.

변수 선언 시 항상 `val`을 사용한다. (단, `var` 사용은 본인의 재량에 맡기며, 이로 인해 발생하는 문제는 해당 선택을 한 사람의 책임이다. 단, 엔티티 클래스의 ID 값은 예외적으로 `var`을 사용한다.)

의존성 주입(DI) 시, 클래스 이름 뒤에 생성자 파라미터 형태로 작성한다.
```kotlin
// 단일 의존성을 받을 때
class CommunityLikeServiceImpl(
  private val communityLikeRepository: CommunityLikeRepository
): CommunityLikeService { ... }

// 두 개 이상의 의존성을 받을 때
class CommunityLikeServiceImpl(
  private val communityLikeRepository: CommunityLikeRepository,
  private val memberRepository: MemberRepository
): CommunityLikeService { ... }
```
Java의 `orElseThrow`를 리팩토링할 때는 `?:`을 사용하며, `?:` 앞에는 항상 줄바꿈을 넣는다.
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
컴파일러가 타입을 추론할 수 있는 경우 타입 선언을 생략한다.
```kotlin
// X — 추론 가능함에도 Member 타입을 명시한 경우
val member: Member = memberRepository.findMemberById(communityLikeRequest.memberId)
            ?: throw ApplicationException(MemberStatusCode.MEMBER_NOT_FOUND)

// O — 추론 가능하므로 Member를 생략한 경우
val member = memberRepository.findMemberById(communityLikeRequest.memberId)
            ?: throw ApplicationException(MemberStatusCode.MEMBER_NOT_FOUND)
```
항상 서비스(인터페이스)를 먼저 생성하고, Impl 클래스를 통해 구현한다.

상속을 통해 구현할 때는 다음과 같이 작성한다:
```kotlin
// 둘 이상 상속받는 경우
class CommunityLikeServiceImpl(
  private val communityLikeRepository: CommunityLikeRepository,
  private val memberRepository: MemberRepository
): CommunityPostLikeService, CommunityPostUnlikeService {

// 하나만 상속받는 경우
class CommunityLikeServiceImpl(
  private val communityLikeRepository: CommunityLikeRepository,
  private val memberRepository: MemberRepository
): CommunityLikeService {
```
b. 상속을 통해 오버라이딩할 때는 `@Override` 어노테이션을 사용하지 않고, 아래와 같이 Kotlin 고유 문법을 사용한다.
```kotlin
override fun like(): CommunityPostLikeResponse {
```
c. 파일 구조는 다음과 같이 한다:
```
service/
├── CommunityLikeService
└── CommunityLikeServiceImpl
```
Builder 패턴을 사용하지 않고, 생성자를 통해 인스턴스를 생성한다.

여기서 다루지 않은 나머지 컨벤션은 Backend Java Convention을 따른다.

### 엔티티 클래스

모든 엔티티 클래스의 ID는 기본적으로 `Long` 타입을 사용한다. (단, 추후 `UUID` 등의 타입으로 리팩토링될 수 있다.)

ID는 `private`으로 선언한다.

ID에 대한 setter는 생성하지 않으며, getter는 다음과 같이 작성한다:
```kotlin
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private var id: Long? = null

fun getId() = id
```

### 파라미터

생성자 파라미터와 메서드 파라미터는 동일한 형식을 따른다.

생성자 파라미터는 여는 괄호 바로 뒤에 항상 줄바꿈을 넣는다.

닫는 괄호 뒤에 상위 클래스/인터페이스가 오는 경우, 콜론(`:`)은 닫는 괄호 바로 뒤에 위치시킨다.

2개 이상 상속받는 경우, 줄바꿈 없이 쉼표(`,`)로 구분한다.
```kotlin
class PostServiceImpl(
    private val postRepository: PostRepository
): PostService {

class PostServiceImpl(
    private val postRepository: PostRepository,
    private val memberRepository: MemberRepository
): PostService {
```

```kotlin
// 메서드의 파라미터가 하나인 경우
fun signUp(
    signUpRequest: SignUpRequest
): SignUpResponse { ... }

// 메서드의 파라미터가 두 개 이상인 경우
fun signIn(
    signInRequest: SignInRequest,
    httpServletResponse: HttpServletResponse
): SignInResponse { ... }
```