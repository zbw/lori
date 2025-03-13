package de.zbw.api.lori.server.connector

sealed class ApiResponse<out T, out E> {
    /**
     * Represents successful network responses (2xx).
     */
    data class Success<T>(
        val body: T,
    ) : ApiResponse<T, Nothing>()

    sealed class Error<E> : ApiResponse<Nothing, E>() {
        /**
         * Represents server (50x) and client (40x) errors.
         */
        data class HttpError<E>(
            val code: Int,
            val errorBody: E?,
        ) : Error<E>()

        /**
         * Represent IOExceptions and connectivity issues.
         */
        data class NetworkError(
            val message: String,
        ) : Error<Nothing>()

        /**
         * Represent SerializationExceptions.
         */
        data class SerializationError(
            val message: String,
        ) : Error<Nothing>()
    }
}
