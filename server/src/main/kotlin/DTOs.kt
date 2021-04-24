data class UserInfo(
        val password: String // best security ever
)

data class RequestContext(
        val curTime: Long,
        val userName: String
)

data class APIUserInfo(
        val userName: String,
        val password: String
)

interface APIUserAction {
    val user: APIUserInfo
}

data class APIOnlyUser(
        override val user: APIUserInfo
) : APIUserAction

data class APISendAnswer(
        override val user: APIUserInfo,
        val answer: String,
        val theme: Int,
        val problem: Int
) : APIUserAction

data class APIGetState(
        val curTime: Long,
        val points: Int,
        val problemsCnt: Int,
        val res: List<List<Boolean>>
)
