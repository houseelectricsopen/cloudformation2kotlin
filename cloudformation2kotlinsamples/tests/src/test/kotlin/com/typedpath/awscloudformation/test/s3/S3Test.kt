package com.typedpath.awscloudformation.test.s3

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.ObjectMetadata
import com.typedpath.awscloudformation.CloudFormationTemplate
import com.typedpath.awscloudformation.test.TemplateFactory
import com.typedpath.awscloudformation.test.util.createStack
import org.junit.Assert
import org.junit.Test
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.net.URL
import java.nio.charset.Charset
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class S3Test : TemplateFactory{

    val strDateTime = DateTimeFormatter.ofPattern("ddMMyyyy-HHmmss").format(LocalDateTime.now())
    val bucketName = """s3publicreadabletest-$strDateTime"""


    override fun createTemplate() : CloudFormationTemplate {
        val strDateTime = DateTimeFormatter.ofPattern("ddMMyyyy-HHmmss").format(LocalDateTime.now())
        val bucketName = """s3publicreadabletest-$strDateTime"""
        return  S3PublicReadableCloudFormationTemplate(bucketName)
    }

    private fun readLine(url: URL): String = (BufferedReader(InputStreamReader(url.openStream()))).readLine()

    @Test
    fun publicBucket() {

        val testTemplate = createTemplate()
        val strStackName = """s3testStack$strDateTime"""

        val region = Regions.US_EAST_1

        createStack(testTemplate, strStackName, region, false) { credentialsProvider, outputs ->
            println("""*********testing testing credentials $credentialsProvider*************""")
            try {
                val s3Client = AmazonS3ClientBuilder.standard()
                        .withCredentials(credentialsProvider)
                        .withRegion(region)
                        .build()
                val fileObjKeyName = "ithink.txt"
                val textData = "i think therefore IBM"
                val metadata = ObjectMetadata()
                metadata.setContentType("application/blob")
                metadata.addUserMetadata("x-amz-meta-title", fileObjKeyName)
                val inputStream = ByteArrayInputStream(textData.toByteArray(Charset.defaultCharset()))
                println("""putting text "$textData" to "$fileObjKeyName"  in bucket $bucketName """)
                s3Client.putObject(bucketName, fileObjKeyName, inputStream, metadata)

                val url = "https://$bucketName.s3.amazonaws.com/$fileObjKeyName"
                println("reading from $url")
                var readBack = readLine(URL(url))

                println("""read from $url: "$readBack" """)
                Assert.assertEquals(textData, readBack)

                s3Client.deleteObject(bucketName, fileObjKeyName)

            } catch (e: Exception) {
                error("" + e.message)
                throw RuntimeException("failed s3 createStack", e)
            }
        }
    }
}

fun main(args: Array<String>) {
    S3Test().publicBucket()
}