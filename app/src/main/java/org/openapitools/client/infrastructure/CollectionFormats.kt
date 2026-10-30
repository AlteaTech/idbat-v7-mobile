package org.openapitools.client.infrastructure

class CollectionFormats {

    open class CSVParams {

        var params: List<String>

        constructor(params: List<String>) {
            this.params = params
        }

        constructor(vararg params: String) {
            this.params = listOf(*params)
        }

        override fun toString(): String {
            return params.joinToString(",")
        }
    }

    open class SSVParams : CSVParams {

        constructor(params: List<String>) : super(params)

        constructor(vararg params: String) : super(*params)

        override fun toString(): String {
            return params.joinToString(" ")
        }
    }

    open class TSVParams : CSVParams {

        constructor(params: List<String>) : super(params)

        constructor(vararg params: String) : super(*params)

        override fun toString(): String {
            return params.joinToString("\t")
        }
    }

    open class PIPESParams : CSVParams {

        constructor(params: List<String>) : super(params)

        constructor(vararg params: String) : super(*params)

        override fun toString(): String {
            return params.joinToString("|")
        }
    }

    open class SPACEParams : SSVParams()
}
