package huskybot.modules.cmdHelpers

import huskybot.cmdFramework.Context
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild.Ban
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.requests.RestAction
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * Object class that allows the reuse of code across the bot,
 * as well as ensures that only the correct user has access to
 * moderator actions.
 */
object ModHelper {

    /**
     * Ban helper that returns the outcome of a ban attempt.
     * @param ctx Context object
     * @param member Member that is to be banned
     * @param reason Reason for ban, will be logged if modlog is enabled
     * @param duration How long the ban will last in days
     * @param delDays How many days back worth of the user's messages that will be deleted
     * @return Result of the ban
     */
    fun tryBan(ctx: Context, member: Member,
               reason: String, duration: Int,
               delDays: Int) : CompletableFuture<Result> {

        val self = ctx.guild?.selfMember
        val moderator = ctx.member

        /* Pre-Ban Checks */

        if(!self?.hasPermission(Permission.BAN_MEMBERS)!!) {
            return CompletableFuture.supplyAsync{Result.BOT_NO_PERMS}       //Bot lacks ban permission
        }

        if(!moderator.hasPermission(Permission.BAN_MEMBERS)) {
            return CompletableFuture.supplyAsync{Result.USER_NO_PERMS}      //Moderator lacks ban permission
        }

        if(!self.canInteract(member)) {
            return CompletableFuture.supplyAsync{Result.MEMBER_TOO_HIGH}    //Member is above the user or bot
        }

        /* Attempt ban */

        try {
            ctx.guild.ban(member, delDays, TimeUnit.DAYS)
                .reason(reason)
                .queue()
        } catch (e: Exception) {
            throw e
        }

        /* Log the ban in the modlog */
        //Throw modlog code here

        return CompletableFuture.supplyAsync{Result.SUCCESS}
    }

    /**
     * Helper method for unbanning a user, which returns the result of the unban attempt.
     * @param ctx Context object
     * @param user User that is to be unbanned
     * @param reason Reason for unban, will be logged if modlog is enabled
     * @return Result of unban attempt
     */
    fun tryUnban(ctx: Context, user: User, reason: String) : CompletableFuture<Result> {

        val self = ctx.guild?.selfMember
        val moderator = ctx.member

        /* Pre-Run Checks */

        if(!self?.hasPermission(Permission.BAN_MEMBERS)!!) {
            return CompletableFuture.supplyAsync{Result.BOT_NO_PERMS}       //Bot lacks ban permission
        }

        if(!moderator.hasPermission(Permission.BAN_MEMBERS)) {
            return CompletableFuture.supplyAsync{Result.USER_NO_PERMS}      //Moderator lacks ban permission
        }

        if (ctx.jda.getUserById(user.idLong) != null) {
            try {
                ctx.guild.retrieveBan(user).queue(null, RestException().onFail())
            } catch (e: java.lang.Exception) {
                return CompletableFuture.supplyAsync { Result.MEMBER_NOT_BANNED }  //Member is not banned
            }
        }

        /* Log the action in the modlog */
        //Throw modlog code here

        /* Unban the user */
        ctx.guild.unban(user)
            .reason(reason)
            .queue()

        return CompletableFuture.supplyAsync{Result.SUCCESS}
    }

    /**
     * Helper method for kicking a user, which returns the result of the kick.
     * @param ctx Context object
     * @param member Member that is to be kicked
     * @param reason Reason for the kick, will be logged if modmail is enabled
     * @return Result of the kick attempt
     */
    fun tryKick(ctx: Context, member: Member, reason: String) : CompletableFuture<Result> {

        val self = ctx.guild?.selfMember
        val moderator = ctx.member

        /* Pre-Run Checks */

        if(!self?.hasPermission(Permission.KICK_MEMBERS)!!) {
            return CompletableFuture.supplyAsync{Result.BOT_NO_PERMS}       //Bot lacks kick permission
        }

        if(!moderator.hasPermission(Permission.KICK_MEMBERS)) {
            return CompletableFuture.supplyAsync{Result.USER_NO_PERMS}      //Moderator lacks kick permission
        }

        if(!self.canInteract(member)) {
            return CompletableFuture.supplyAsync{Result.MEMBER_TOO_HIGH}    //Member is above the user or bot
        }

        /* Attempt kick */

        try {
            ctx.guild.kick(member)
                .reason(reason)
                .queue()
        } catch (e: Exception) {
            throw e
        }

        /* Log the action in the modlog */
        //Throw modlog code here

        return CompletableFuture.supplyAsync{Result.SUCCESS}
    }

    fun tryWarn(ctx: Context, member: Member, reason: String) : CompletableFuture<Result> {

        val self = ctx.guild?.selfMember
        val moderator = ctx.member

        /* Pre-Run Checks */

        if(!self?.hasPermission(Permission.KICK_MEMBERS)!!) { //permission to kick and permission to warn are one in the same
            return CompletableFuture.supplyAsync{Result.BOT_NO_PERMS}       //Bot lacks kick permission
        }

        if(!moderator.hasPermission(Permission.KICK_MEMBERS)) {
            return CompletableFuture.supplyAsync{Result.USER_NO_PERMS}      //Moderator lacks kick permission
        }

        if(!self.canInteract(member)) {
            return CompletableFuture.supplyAsync{Result.MEMBER_TOO_HIGH}    //Member is above the user or bot
        }

        /* Warn the User */
        //TODO( "Still needs to log the warning. Only sends a DM to user as of now")

        ctx.jda.openPrivateChannelById(member.user.idLong)
                .queue{channel ->
                    channel.sendMessage("You have been warned for... \n" + reason).queue()
                }


        /* Log the action in the modlog */
        //Throw modlog code here

        return CompletableFuture.supplyAsync{Result.SUCCESS}
    }
}

/**
 * Enum class for categorizing result types
 */
enum class Result {
    SUCCESS,                //Success
    BOT_NO_PERMS,           //Bot lacks permission
    USER_NO_PERMS,          //User lacks permission
    MEMBER_TOO_HIGH,        //Member is above the user or bot
    MEMBER_NOT_BANNED;      //Member is/was not banned
}