import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.gson.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.util.pipeline.*


fun Application.main() {
    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
            disableHtmlEscaping()
        }
    }

    val gs = Game()

    routing {
        get("/") {
            call.respondRedirect("/index.html")
        }

        static("/") {
            resources("/")
        }

        get("/api/get") {
            val userName = call.parameters["userName"]!!
            val ctx = RequestContext(getCurrentTS(), userName)
            if (gs.verifyUser(ctx, call.parameters["password"]!!)) {
                call.respond(gs.userGet(ctx))
            } else {
                call.respond(HttpStatusCode.Forbidden, "access denied")
            }
        }

        post("api/login") {
            doResponse<APIOnlyUser>(gs, { isGoodStr(user.password) && isVeryGoodStr(user.userName) }) { ctx ->
                gs.logIn(ctx, user.password)
            }
        }

        post("/api/sendAnswer") {
            doResponse<APISendAnswer>(gs, { isGoodStr(answer) }, isUser = true) { ctx ->
                gs.sendAnswer(ctx, theme, problem, answer)
            }
        }

        post("/api/startGame") {
            doResponse<APIOnlyUser>(gs, { true }, isAdmin = true) { ctx ->
                gs.startGame(ctx)
            }
        }

        post("/api/addAnswer") {
            doResponse<APISendAnswer>(gs, { isGoodStr(answer) }, isAdmin = true) { ctx ->
                gs.addAnswer(ctx, theme, problem, answer)
            }
        }

        post("/api/deleteAnswer") {
            doResponse<APISendAnswer>(gs, { isGoodStr(answer) }, isAdmin = true) { ctx ->
                gs.deleteAnswer(ctx, theme, problem, answer)
            }
        }

        /*post("api/redirectToGetResult") {
            val arg = call.receive<APIOnlyUser>()
            val ctx = RequestContext(getCurrentTS(), arg.user.userName)
            if (!gs.verifyAdmin(ctx, arg.user.password)) {
                call.respond(HttpStatusCode.Forbidden, "Only admin can erase results")
                return@post
            }
            call.respondText("/api/getResults?token=${gs.tokenForResult}")
        }

        get("/api/getResults") {
            val token = call.parameters["token"]!!
            if (token != gs.tokenForResult) {
                call.respond(HttpStatusCode.Forbidden, "Invalid token")
                return@get
            }
            call.response.header(HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, "results.csv").toString())
            call.respondTextWriter { gs.writeResult(this) }
        }*/
    }
}

private suspend inline fun <reified T : APIUserAction> PipelineContext<Unit, ApplicationCall>.doResponse(
        gs: Game,
        verifier: T.() -> Boolean,
        isUser: Boolean = false,
        isAdmin: Boolean = false,
        action: T.(RequestContext) -> ActionResult
) {
    val arg = call.receive<T>()
    val ctx = RequestContext(getCurrentTS(), arg.user.userName)
    if ((!isUser || gs.verifyUser(ctx, arg.user.password))
            && (!isAdmin || verifyAdmin(ctx, arg.user.password))
            && verifier(arg)
    ) {
        call.respond(action(arg, ctx))
    } else {
        call.respond(false)
    }
}
