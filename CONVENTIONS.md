## 공통

중괄호 시작 후 항상 한 줄 줄바꿈 후 코드 작성

```kotlin
@Service
@Transactional(rollbackFor = [Exception::class])
class MemberServiceImpl: MemberService {

    override fun deleteAccount
```

파일 내 코드 마지막 부분에서 중괄호 닫은 이후 줄 바꿈 금지
```kotlin
		return ApiResponse.ok("");
	}
}
```


모든 리턴은 BaseResponse를 이용한 공통 응답 반환 및 response활용
```kotlin
return BaseResponse.ok(SignUpResponse.of("가입되었습니다."));
```


소괄호 내 값이 2개 이상인 경우 줄바꿈
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


매개변수는 클래스 이름을 카멜케이스로 표기한다.

어노테이션은 길이가 긴걸 아래에 위치시킨다.

return문은 항상 줄바꿈 이후 사용

매개변수에는 HttpServletRequest같이 스프링 부트에서 기본적으로 지원하는 애들 보다 DTO가 더 앞에 사용되도록 해주세요.



변수를 선언할 때는 항상 val을 사용한다.(단, 본인의 판단하에 var을 사용해도 되나 이에 따른 불이익은 사용자 본인에게 있다.)

DI(의존성 주입)하는 경우 클래스명 뒤에 작성하기에 매개변수 취급한다.
```kotlin
// 의존성을 하나만 받는 경우
class CommunityLikeImpl(
  private val communityLikeRepository: CommunityLikeRepository
): { ... }

// 의존성을 두개 이상 받는 경우
class CommunityLikeImpl(
  private val communityLikeRepository: CommunityLikeRepository,
  private val memberRepository: MemberRepository
): { ... }
```
자바에서 orElseThrow를 사용한 경우 ?: 를 이용해 리팩토링 하며, ?:를 사용하기 전에 항상 줄바꿈 한다.
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
컴파일러에서 타입 추론이 가능한 경우 타입 선언은 생략한다.
```kotlin
// 타입 추론이 가능하지만 Member 타입을 선언한 경우 X
val member: Member = memberRepository.findMemberById(communityLikeRequest.memberId)
            ?: throw ApplicationException(MemberStatusCode.MEMBER_NOT_FOUND)

// 타입 추론이 가능하기에 Member를 생략 O
val member = memberRepository.findMemberById(communityLikeRequest.memberId)
            ?: throw ApplicationException(MemberStatusCode.MEMBER_NOT_FOUND)
```
항상 유스케이스(인터페이스)를 만들고 한 대 묶어 구현체(서비스)를 만들어야 한다.

상속을 받아 구현할 때는 아래와 같이 작성한다.
```kotlin
// 두 개 이상을 상속 받아 구현할 때
class CommunityLikeImpl(
  private val communityLikeRepository: CommunityLikeRepository,
  private val memberRepository: MemberRepository
): CommunityPostLikeUseCase, CommunityPostUnlikeUseCase {

// 하나만 상속 받아 구현할 때
class CommunityLikeImpl(
  private val communityLikeRepository: CommunityLikeRepository,
  private val memberRepository: MemberRepository
): CommunityPostLikeUseCase {
```
b. 상속 받아 오버라이딩을 할 때 @Override 어노테이션을 사용하지 않고 코틀린 기본 문법을 아래와 같이 사용한다.
```kotlin
override fun like(): CommunityPostLikeResponse {
```
c. 파일 구조는 아래와 같이 한다.
```
service/
└── usecase/
│   ├── CommunityPostLikeUseCase
│   └── CommunityPostUnlikeUseCase
└── CommunityLikeImpl
```
Builder 패턴을 사용하지 않고, 생성자를 이용해 생성한다.

그 외 모든 컨밴션은 Backend Java Convention과 동일시 한다.

엔티티 클래스

모든 엔티티 클래스의 ID는 우선적으로 Long타입을 사용한다.(단, 추후 리팩토링 시에 UUID등과 같은 타입으로 변경될 수 있다.)

ID는 private을 사용한다.

ID의 setter는 만들지 않으며 getter는 아래와 같이 만든다.
```kotlin
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private val id: Long? = null

fun getId() = id
```
매개변수

생성자 매개변수와 메서드내 매개변수의 형식은 같다

생성자 매개변수는 소괄호 이후 항상 줄바꿈을 한다.

끝마치는 소괄호 이후에 상속받는 것이 있다면 콜론(:)을 닫는 소괄호 바로 뒤에 작성한다.

2개 이상을 상속받는 경우 쉼표(,)를 사용해 구분하되 줄바꿈하지 않는다.
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
// 메서드내 메개변수가 하나인 경우
public SignUpResponse signUp(
    SignUpRequest signUpRequest
) { ... }

//메서드내 매개변수가 두 개 이상인 경우
public SignInResponse signIn(
    SignInRequest signInRequest,
    HttpServletResponse httpServletResponse
) { ... }
```