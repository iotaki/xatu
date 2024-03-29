package com.theoxao.base.script

import com.fasterxml.jackson.databind.ObjectMapper
import com.theoxao.base.persist.model.RouteScript
import com.theoxao.base.route.handler.RouteHandler
import org.bson.types.ObjectId
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.MessageListener
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.listener.PatternTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor


/**
 * create by theoxao on 2019/5/19
 */
@Configuration
open class RedisMessageListener(private val redisTemplate: StringRedisTemplate) {

    @Bean
    open fun container(routeHandler: RouteHandler): RedisMessageListenerContainer {
        val objectMapper = ObjectMapper()
        val container = RedisMessageListenerContainer()
        container.connectionFactory = redisTemplate.connectionFactory
        container.setTaskExecutor(ConcurrentTaskExecutor())
        val topic = PatternTopic("*")
        container.addMessageListener(MessageListener { message, _ ->
            val body = String(message.body)
            val channel = String(message.channel)
            println(body)
            println(channel)
            when {
                //TODO use hash to store route data
                channel.contains("set") -> {
                    val raw = redisTemplate.boundValueOps(body).get()
                    val node = objectMapper.readTree(raw)
                    val data = RouteScript()
                    data.id = node.findValue("id").asText()
                    data.uri = node.findValue("uri").asText()
                    data.content = node.findValue("content").asText()
                    routeHandler.addRoute(data)
                }
                channel.contains("del") -> {
                    routeHandler.removeRoute(body)
                }
            }
        }, topic)
        return container
    }

}
