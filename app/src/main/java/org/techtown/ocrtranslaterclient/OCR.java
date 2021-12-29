package org.techtown.ocrtranslaterclient;


import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Feature.Type;
import com.google.cloud.vision.v1.Image;
import com.google.protobuf.ByteString;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

public class OCR {

    public static String Request(ByteString imgBytes) {

        try {

            List<AnnotateImageRequest> requests = new ArrayList<>();

            Image img = Image.newBuilder().setContent(imgBytes).build();
            Feature feat = Feature.newBuilder().setType(Type.TEXT_DETECTION).build();
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
            requests.add(request);

            try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
                BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
                List<AnnotateImageResponse> responses = response.getResponsesList();

                for (AnnotateImageResponse res : responses) {
                    if (res.hasError()) {
                        System.out.printf("Error: %s\n", res.getError().getMessage());
                        return " {\"error\":\" "+ res.getError().getMessage() + " \"} ";
                    }

                    System.out.println("Text : ");
                    System.out.println(res.getTextAnnotationsList().toString());

                    // For full list of available annotations, see http://g.co/cloud/vision/docs
			    	/*for (EntityAnnotation annotation : res.getTextAnnotationsList()) {

						//System.out.printf("Text: %s\n", annotation.getDescription());
						//System.out.printf("Position : %s\n", annotation.getBoundingPoly());
					}*/
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
            return " {\"error\":\" "+ e.toString() + " \"} ";
        }
        return null;
    }
}