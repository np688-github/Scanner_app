package com.example.scannerapp


import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage


class MainActivity : AppCompatActivity() {
    lateinit var btnScanBarcode:Button
    lateinit var tvResult: TextView
    lateinit var ivQrCode:ImageView

    private var CAMERA_PERMISSION_CODE = 123
    private var READ_STORAGE_PERMISSION_CODE = 113
    private var WRITE_STORAGE_PERMISSION_CODE = 113
    private var TAG = "MyTag"

    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    lateinit var inputImage: InputImage
    lateinit var barcodeScanner: BarcodeScanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        btnScanBarcode = findViewById(R.id.btnScanBarcode)
        tvResult = findViewById(R.id.tvResult)
        ivQrCode = findViewById(R.id.ivQrCode)

        barcodeScanner = BarcodeScanning.getClient()
        cameraLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            object : ActivityResultCallback<ActivityResult>{
                override fun onActivityResult(result: ActivityResult?) {
                    var data=result?.data

                    try {
                        val photo = data?.extras?.get("data")as Bitmap
                        inputImage = InputImage.fromBitmap(photo,0)
                        processQr()

                    }catch (e:Exception){
                        Log.d(TAG,"onActivityResult:"+ e.message)
                    }
                }

            }
        )
        galleryLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            object : ActivityResultCallback<ActivityResult>{
                override fun onActivityResult(result: ActivityResult?) {
                    var data = result?.data

                    inputImage = InputImage.fromFilePath(this@MainActivity, data?.data!!)
                    processQr()
                }

            }
        )

        btnScanBarcode.setOnClickListener {

            val options = arrayOf("camera","gallery")
            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setTitle("Pick a Option")

            builder.setItems(options,DialogInterface.OnClickListener { dialog, which ->
                if (which == 0){
                    var cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    cameraLauncher.launch(cameraIntent)
                }else{
                    var storageIntent = Intent()
                    storageIntent.setType("image/*")
                    storageIntent.setAction(Intent.ACTION_GET_CONTENT)
                    galleryLauncher.launch(storageIntent)
                }
            })
            builder.show()
        }
    }
    private fun processQr(){
        ivQrCode.visibility = View.GONE
        tvResult.visibility = View.VISIBLE

        barcodeScanner.process(inputImage).addOnSuccessListener {
            for (barCode:Barcode in it){
                val valueType = barCode.valueType

                when (valueType) {
                    Barcode.TYPE_WIFI -> {
                        val ssid = barCode.wifi!!.ssid
                        val password = barCode.wifi!!.password
                        val type = barCode.wifi!!.encryptionType

                        tvResult.text = "ssid ${ssid} \n password ${password} \n type ${type}"
                    }
                    Barcode.TYPE_URL -> {
                        val title = barCode.url!!.title
                        val url = barCode.url!!.url

                        tvResult.text = "ssid ${title} \n password ${url}"
                    }
                    Barcode.TYPE_TEXT -> {
                        val data = barCode.displayValue

                        tvResult.text = "Result ${data}"
                    }
                }

            }
        }.addOnFailureListener {
            Log.d(TAG,"processQr: ${it.message}")
        }
    }

    override fun onResume() {
        super.onResume()

        checkPermission(android.Manifest.permission.CAMERA,CAMERA_PERMISSION_CODE)
    }
    private fun checkPermission(permission:String,requestCode:Int){
        if (ContextCompat.checkSelfPermission(this@MainActivity,permission)== PackageManager.PERMISSION_DENIED){

            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission),requestCode)
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode ==CAMERA_PERMISSION_CODE){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                checkPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE,READ_STORAGE_PERMISSION_CODE)

            }else{
                Toast.makeText(this@MainActivity, "Camera Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }else if (requestCode == READ_STORAGE_PERMISSION_CODE){
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                checkPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE,WRITE_STORAGE_PERMISSION_CODE)

            }else{
                Toast.makeText(this@MainActivity, "Storage Permission Granted", Toast.LENGTH_SHORT)
                    .show()
            }
        }
        else if (requestCode == WRITE_STORAGE_PERMISSION_CODE){
            if (!(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this@MainActivity, "Storage Permission Granted", Toast.LENGTH_SHORT)
                    .show()

            }
        }
    }
}