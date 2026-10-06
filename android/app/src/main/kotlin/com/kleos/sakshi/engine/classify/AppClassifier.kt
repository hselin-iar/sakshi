package com.kleos.sakshi.engine.classify

import com.kleos.sakshi.engine.model.AppClass
import com.kleos.sakshi.engine.model.Pkg
import com.kleos.sakshi.engine.model.UserClass
import com.kleos.sakshi.engine.ports.AppCatalog

class AppClassifier(private val userClasses: Map<Pkg, UserClass>, private val catalog: AppCatalog) {
    fun classify(pkg: Pkg): AppClass {
        if (pkg == catalog.ownPackage()) return AppClass.NEUTRAL

        when (userClasses[pkg]) {
            UserClass.IN_SET -> return AppClass.IN_SET
            UserClass.DEPENDS -> return AppClass.DEPENDS
            UserClass.NONE, null -> Unit
        }

        if (catalog.isNeutral(pkg)) return AppClass.NEUTRAL
        return AppClass.OFF_SET
    }
}
