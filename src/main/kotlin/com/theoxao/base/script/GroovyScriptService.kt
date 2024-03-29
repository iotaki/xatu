package com.theoxao.base.script

import com.theoxao.base.common.GroovyShellHolder
import com.theoxao.base.common.SyntaxType
import com.theoxao.base.persist.repository.ScriptRepository
import com.theoxao.base.script.ast.ApiASTTransform.Companion.API_META_FIELD_NAME
import com.theoxao.base.script.ast.ApiASTTransform.Companion.API_META_OBJECT_NAME
import com.theoxao.base.script.ast.AutowiredASTTransform
import com.theoxao.base.script.ast.MetaApi
import groovy.lang.Script
import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service

/**
 * create by theoxao on 2019/5/19
 */
@Service
class GroovyScriptService(
    private val scriptRepository: ScriptRepository,
    private val applicationContext: ApplicationContext
) {
    private val shell = GroovyShellHolder.shell

    fun parse(scriptAsString: String, invoke: Script.() -> Any): Any {
        return invoke(parse(scriptAsString))
    }

    fun parse(scriptAsString: String): Script = shell.parse(scriptAsString)

    fun parseAndAutowire(scriptAsString: String): Script {
        val script = this.parse(scriptAsString)
        script.metaClass.properties.forEach {
            if (it.name.endsWith(AutowiredASTTransform.AUTOWIRE_BEAN_SUFFIX)) {
                val type = it.type
                val bean: Any = try {
                    applicationContext.getBean(it.name.removeSuffix(AutowiredASTTransform.AUTOWIRE_BEAN_SUFFIX))
                } catch (ignore: BeansException) {
                    applicationContext.getBean(type)
                }
                if (type.isAssignableFrom(bean::class.java)) {
                    script.metaClass.setProperty(
                        script,
                        it.name.removeSuffix(AutowiredASTTransform.AUTOWIRE_BEAN_SUFFIX),
                        bean
                    )
                }
            }
            if (it.name == API_META_FIELD_NAME) {
                val rawMeta = it.getProperty(script) as String
                script.metaClass.setProperty(script, API_META_OBJECT_NAME, MetaApi.fromString(rawMeta))
            }
        }
        return script
    }

    fun mapParameterScript(scriptAsString: String, methodName: String, param: Map<Any, Any>): Any {
        val script = shell.parse(scriptAsString)
        return script.invokeMethod(methodName, param)
    }

    fun preParse(rawScript: String, type: SyntaxType): String {
        val bean = applicationContext.getBean(type.preParser ?: throw RuntimeException("pre-parser not found"))
        return bean.preParse(rawScript)
    }

    fun findScriptById(id: String) = scriptRepository.findById(id) ?: throw RuntimeException("script not found")
}
