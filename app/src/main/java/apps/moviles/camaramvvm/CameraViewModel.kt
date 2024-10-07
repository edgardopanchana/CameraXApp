package apps.moviles.camaramvvm
import androidx.lifecycle.ViewModel
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.nio.ByteBuffer

class CameraViewModel : ViewModel() {

    private val _imageBitmap = MutableLiveData<Bitmap?>()
    val imageBitmap: LiveData<Bitmap?> get() = _imageBitmap

    // Definimos una variable privada y mutable de tipo LiveData que contendrá un objeto ImageCapture.
    private val _imageCapture = MutableLiveData<ImageCapture>()

    // Creamos una propiedad pública de solo lectura para acceder a la variable privada _imageCapture,
// que expone el LiveData de tipo ImageCapture.
    val imageCapture: LiveData<ImageCapture> get() = _imageCapture

    // Función que configura la cámara para mostrar una vista previa en el PreviewView proporcionado.
    fun setupCamera(context: Context, previewView: PreviewView) {

        // Obtenemos una instancia de ProcessCameraProvider para acceder a las funciones de la cámara.
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        // Añadimos un listener que se ejecuta cuando el proveedor de la cámara está disponible.
        cameraProviderFuture.addListener({

            // Obtenemos el cameraProvider, que nos permite controlar la cámara.
            val cameraProvider = cameraProviderFuture.get()

            // Configuramos la vista previa de la cámara con un Builder y la construimos.
            val preview = androidx.camera.core.Preview.Builder().build()

            // Creamos un objeto ImageCapture, que permite capturar imágenes.
            val imageCapture = ImageCapture.Builder().build()

            // Seleccionamos la cámara trasera por defecto.
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // Establecemos el proveedor de la superficie para que la vista previa se muestre en el PreviewView.
            preview.setSurfaceProvider(previewView.surfaceProvider)

            try {
                // Desvinculamos cualquier uso anterior de la cámara.
                cameraProvider.unbindAll()

                // Vinculamos el ciclo de vida de la cámara al LifecycleOwner (el contexto en este caso),
                // con el selector de cámara (cámara trasera), la vista previa y la funcionalidad de captura de imagen.
                cameraProvider.bindToLifecycle(
                    context as LifecycleOwner, // Aseguramos que el contexto sea un LifecycleOwner.
                    cameraSelector, // Selección de la cámara trasera.
                    preview, // Vista previa de la cámara.
                    imageCapture // Objeto para capturar imágenes.
                )

                // Asignamos el objeto imageCapture a la variable _imageCapture para que esté disponible a través de LiveData.
                _imageCapture.value = imageCapture

            } catch (e: Exception) {
                // Si ocurre un error al vincular la cámara, lo registramos en los logs.
                Log.e("CameraViewModel", "Camera binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context)) // Ejecutamos el listener en el hilo principal.
    }

    fun captureImage(context: Context) {
        val imageCapture = _imageCapture.value ?: return

        imageCapture.takePicture(
            androidx.core.content.ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    val bitmap = imageProxy.toBitmap()
                    _imageBitmap.value = bitmap
                    imageProxy.close()  // Cerrar el ImageProxy después de usarlo
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraViewModel", "Error al capturar imagen", exception)
                }
            }
        )
    }

}

private fun ImageProxy.toBitmap(): Bitmap {
    val buffer: ByteBuffer = planes[0].buffer
    buffer.rewind()
    val bytes = ByteArray(buffer.capacity())
    buffer.get(bytes)
    return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
}
