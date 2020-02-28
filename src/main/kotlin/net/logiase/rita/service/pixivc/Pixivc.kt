package net.logiase.rita.service.pixivc

import kotlinx.coroutines.*
import net.logiase.rita.conf.Conf
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.sendMessage
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.sendTo
import net.mamoe.mirai.message.sendAsImageTo
import net.mamoe.mirai.message.uploadAsImage
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.Exception
import java.net.URL

object Pixivc {

    // Api服务
    private val service: PixivcApiService by lazy {
        Retrofit.Builder()
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("https://api.pixivic.com/")
            .build()
            .create(PixivcApiService::class.java)
    }

    suspend fun searchIllust(keyword: String, contact: Contact) {
        withTimeoutOrNull(10 * 1000) {
            withContext(Dispatchers.IO) {
                contact.bot.logger.info("start service")
                val response = service.search(keyword = keyword)
                if (response.data.isNullOrEmpty()) {
                    contact.sendMessage("无结果")
                    return@withContext
                }
                try {
                    for (url in response.data.random().imageUrls) {
                        contact.bot.logger.info("start upload")
                        URL(url.original.replace("pximg.net", "pixiv.cat"))
                            .sendAsImageTo(contact)
                            .recallIn(60 * 1000)
                    }
                } catch (e: Exception) {
                    contact.bot.logger.info(e)
                }
            }
        }

    }

}

suspend fun Bot.pixivc() {
    this.subscribeGroupMessages {
        startsWith("pixivc ", removePrefix = true) {
            GlobalScope.launch {
                if (Conf.checkPermission(sender.id) != -1) {
                    bot.logger.info("into pixivc")
                    Pixivc.searchIllust(it, group)
                }
            }
        }
    }
}