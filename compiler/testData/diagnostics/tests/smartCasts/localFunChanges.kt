// IGNORE_IF_NEW_INFERENCE_ENABLED

fun trans(n: Int, f: () -> Boolean) = if (f()) n else null

fun foo() {
    var i: Int? = 5    
    if (i != null) {
        fun can(): Boolean {
            i = null
            return true
        }
        <!SMARTCAST_IMPOSSIBLE!>i<!>.hashCode()
        trans(<!SMARTCAST_IMPOSSIBLE!>i<!>, ::can)
        <!SMARTCAST_IMPOSSIBLE!>i<!>.hashCode()
    }
}
