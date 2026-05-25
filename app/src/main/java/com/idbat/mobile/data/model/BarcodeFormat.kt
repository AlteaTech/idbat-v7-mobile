package com.idbat.mobile.data.model

import com.google.mlkit.vision.barcode.common.Barcode

fun Int.toFormatName() = when (this) {
    Barcode.FORMAT_QR_CODE     -> "QR Code"
    Barcode.FORMAT_EAN_13      -> "EAN-13"
    Barcode.FORMAT_EAN_8       -> "EAN-8"
    Barcode.FORMAT_CODE_128    -> "Code 128"
    Barcode.FORMAT_CODE_39     -> "Code 39"
    Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
    Barcode.FORMAT_PDF417      -> "PDF417"
    Barcode.FORMAT_AZTEC       -> "Aztec"
    Barcode.FORMAT_UPC_A       -> "UPC-A"
    Barcode.FORMAT_UPC_E       -> "UPC-E"
    else                       -> "Code Barre"
}
