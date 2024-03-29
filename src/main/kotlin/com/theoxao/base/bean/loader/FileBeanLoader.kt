package com.theoxao.base.bean.loader

import com.theoxao.base.bean.BeanInjector
import com.theoxao.base.common.GroovyShellHolder
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component


/**
 * @author theo
 * @date 2019/6/24
 */
@Component
class FileBeanLoader(beanInjector: BeanInjector, private val applicationContext: ApplicationContext) :
    BeanLoader(beanInjector) {

    init {
//        inject()
    }

    override fun beanClass(): Class<*> {
        val fileSupplier = fileSupplier()
        val script = GroovyShellHolder.shell.parse(fileSupplier)
        return script.metaClass.theClass
    }

    private fun fileSupplier(): String? {
        val resource = this.javaClass.classLoader.getResource("TestBean.groovy")
        return resource?.readText()

    }

}
