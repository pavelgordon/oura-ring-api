import dev.gordon.ouraringapi.OuraRingApi
import dev.gordon.ouraringapi.OuraRingApiException
import org.junit.Test

internal class OuraRingApiTest{

    @Test(expected = OuraRingApiException::class)
    fun `throw an exception when wrong api key is provided`(){
        val api = OuraRingApi("somekey")
        api.getSleep();
    }
}