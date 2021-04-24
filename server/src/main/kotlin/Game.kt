import kotlinx.coroutines.*
import java.io.File
import java.io.Writer
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.Connection
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

typealias ActionResult = Boolean

const val GAME_TIMER = 62_000_000L // 62m

class Submits(themesCnt: Int) {
    val themes: List<MutableList<String>>
    val isOk: List<MutableList<Boolean>>

    init {
        themes = mutableListOf()
        isOk = mutableListOf()
        for (i in 0 until themesCnt) {
            themes += mutableListOf<String>()
            isOk += mutableListOf<Boolean>()
        }
    }
}

fun simplifyAns(ans: String): String {
    return ans.trim().toLowerCase()
}

fun verifyAdmin(ctx: RequestContext, password: String): Boolean {
    return ctx.userName == "admin" && password == "58302154116"
}

class Game {
    private val users = ConcurrentHashMap<String, UserInfo>()
    private val submits = ConcurrentHashMap<String, Submits>()
    private val results = ConcurrentHashMap<String, Int>()

    private val themesCnt: Int
    private val problemsCnt: Int
    private val answers: List<List<MutableSet<String>>>

    @Volatile
    private var startTime = -1L

    val tokenForResult = Random.Default.nextLong(0, 1_000_000_000_000_000_000).toString()

    init {
        val fp = Paths.get(".")
        println(fp.toAbsolutePath().toString())
        File("answers.csv").bufferedReader().use { r ->
            var line = r.readLine()
            var cells = line.split(";")
            themesCnt = cells[0].toInt()
            problemsCnt = cells[1].toInt()
            val curAnswers = mutableListOf<MutableList<MutableSet<String>>>()
            for (t in 0 until themesCnt) {
                line = r.readLine()
                cells = line.split(";")
                curAnswers += mutableListOf<MutableSet<String>>()
                for (p in 0 until problemsCnt) {
                    curAnswers.last() += mutableSetOf(simplifyAns(cells[p]))
                }
            }
            answers = curAnswers
        }
    }

    fun logIn(ctx: RequestContext, password: String): ActionResult {
        if (verifyAdmin(ctx, password)) {
            return true
        }
        if (users.putIfAbsent(ctx.userName, UserInfo(password)) == null) {
            submits.putIfAbsent(ctx.userName, Submits(themesCnt))
            results.putIfAbsent(ctx.userName, 0)
            return true
        }
        return false
    }

    fun verifyUser(ctx: RequestContext, password: String): Boolean {
        val user = users[ctx.userName] ?: return false
        return user.password == password
    }

    private fun checkTheme(userSubmits: Submits, theme: Int): Boolean {
        return userSubmits.isOk[theme].all { it }
    }

    private fun checkProblem(userSubmits: Submits, problem: Int): Boolean {
        return userSubmits.isOk.all {
            it.size > problem && it[problem]
        }
    }

    private fun rejudge(ctx: RequestContext): ActionResult {
        submits.forEach(4) { user, userSubmits ->
            synchronized(userSubmits) {
                var res = 0
                for (t in 0 until themesCnt) {
                    for (p in 0 until userSubmits.themes[t].size) {
                        userSubmits.isOk[t][p] = answers[t][p].contains(userSubmits.themes[t][p])
                        if (userSubmits.isOk[t][p]) {
                            res += p + 1
                        }
                    }
                    if (userSubmits.themes[t].size == problemsCnt && checkTheme(userSubmits, t)) {
                        res += 5
                    }

                }
                for (p in 0 until problemsCnt) {
                    if (checkProblem(userSubmits, p)) {
                        res += p + 1
                    }
                }
                results[user] = res
            }
        }
        return true
    }

    fun sendAnswer(ctx: RequestContext, theme: Int, problem: Int, answer1: String): ActionResult {
        if (ctx.curTime > startTime + GAME_TIMER) {
            return false
        }
        val answer = simplifyAns(answer1)
        val userSubmits = submits[ctx.userName]!!
        synchronized(userSubmits) {
            if (userSubmits.themes[theme].size != problem) {
                return false
            }
            userSubmits.themes[theme] += answer
            userSubmits.isOk[theme] += answers[theme][problem].contains(answer)
            if (userSubmits.isOk[theme][problem]) {
                results[ctx.userName] = results[ctx.userName]!! + problem + 1
                if (problem == problemsCnt - 1 && checkTheme(userSubmits, theme)) {
                    results[ctx.userName] = results[ctx.userName]!! + 5
                }
                if (checkProblem(userSubmits, problem)) {
                    results[ctx.userName] = results[ctx.userName]!! + problem + 1
                }
            }
            return true
        }
    }

    fun userGet(ctx: RequestContext): APIGetState {
        val userSubmits = submits[ctx.userName]!!
        synchronized(userSubmits) {
            return APIGetState(GAME_TIMER + startTime - ctx.curTime, results[ctx.userName]!!, problemsCnt, userSubmits.isOk)
        }
    }

    fun startGame(ctx: RequestContext): ActionResult {
        if (ctx.curTime - startTime > GAME_TIMER) {
            startTime = ctx.curTime
            return true
        }
        return false
    }

    fun addAnswer(ctx: RequestContext, theme: Int, problem: Int, answer: String): ActionResult {
        return answers[theme][problem].add(simplifyAns(answer))
    }

    fun deleteAnswer(ctx: RequestContext, theme: Int, problem: Int, answer: String): ActionResult {
        return answers[theme][problem].remove(simplifyAns(answer))
    }

    suspend fun writeResult(w: Writer) = coroutineScope {
        //w.write("Name")
    }
}
