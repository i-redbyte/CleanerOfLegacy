package su.redbyte.cleaneroflegacy.annotation

@Retention(AnnotationRetention.SOURCE)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.EXPRESSION
)
annotation class RemoveAfter(val version: String)