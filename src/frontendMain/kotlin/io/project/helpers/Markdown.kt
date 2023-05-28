package io.project.helpers

import io.kvision.require

@Suppress("UnsafeCastFromDynamic")
val marked: (String, dynamic) -> String = require("marked")
