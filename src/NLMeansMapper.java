import hipi.image.FloatImage;
import hipi.image.ImageHeader;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.*;
import java.util.Arrays;
import java.util.ArrayList;
import org.apache.hadoop.conf.Configuration;

//
// NLMeansMap
//
// Mapper class for the NL-means algorithm
//

/**
 * nlmeans_seq
 *
 * Non-Local Means algorithm for 24-bit color images
 * Stores the denoised image in a specified array of 8-bit integers
 *
 * Parameters
 *   input_data    - 2D array of 8-bit numbers containing the image color values
 *   width         - the width of the image
 *   height        - the height of the image
 *   radio_search  - radio of search window (t)
 *   radio_sim     - radio of similarity window (f)
 *   degree        - degree of filtering (h)
 *   output_data   - 2D array of 8-bit numbers that will store processed values
 *   timer         - the timer to use for performance measurements
 */
public class NLMeansMapper extends
        Mapper<ImageHeader, FloatImage, IntWritable, FloatImage> {

    public void map(ImageHeader key, FloatImage value, Context context) {
        Configuration conf = context.getConfiguration();
        int radio_sim = Integer.parseInt(conf.get("radio_sim"));
        int radio_search = Integer.parseInt(conf.get("radio_search"));
        int degree = Integer.parseInt(conf.get("degree"));
        int height = value.getHeight();
        int width = value.getWidth();
        int channels = value.getBands();
        FloatImage newImage = new FloatImage(width, height, channels);
        int padded_input_width = 2 * radio_sim + value.getWidth();
        int sim_diam = 2*radio_sim + 1;
        //int sim_size = sim_diam * sim_diam;

        float[] kernel = generate_kernel_seq(radio_sim);

        float[] input_red, input_green, input_blue;
        input_red = generate_padded_color_array(value, 0, radio_sim, radio_sim, 0);
        input_green = generate_padded_color_array(value, 1, radio_sim, radio_sim, 0);
        input_blue = generate_padded_color_array(value, 2, radio_sim, radio_sim, 0);

        for(int j = 0; j < height; j++) {
            for(int i = 0; i < width; i++) {

                // Get indexes within padded color channel arrays
                int i1 = i + radio_sim;
                int j1 = j + radio_sim;


                float[] sim_window_1_red, sim_window_1_green, sim_window_1_blue;
                // Populate the similarity window for each color channel
                sim_window_1_red = populate_sim_window_seq(radio_sim, input_red, padded_input_width, i1, j1);
                sim_window_1_green = populate_sim_window_seq(radio_sim, input_green, padded_input_width, i1, j1);
                sim_window_1_blue = populate_sim_window_seq(radio_sim, input_blue, padded_input_width, i1, j1);


                // Do denoising for each color channel
                float denoised_red_pixel = compute_denoised_pixel_seq(input_red, i1, j1, value,
                        sim_window_1_red, radio_search, radio_sim, degree, kernel);
                newImage.setPixel(i,j,0,denoised_red_pixel);

                float denoised_green_pixel = compute_denoised_pixel_seq(input_green, i1, j1, value,
                        sim_window_1_green, radio_search, radio_sim, degree, kernel);
                newImage.setPixel(i,j,1,denoised_green_pixel);
                float denoised_blue_pixel = compute_denoised_pixel_seq(input_blue, i1, j1, value,
                        sim_window_1_blue, radio_search, radio_sim, degree, kernel);
                newImage.setPixel(i,j,2,denoised_blue_pixel);
            }
        }

        try {
            context.write(new IntWritable(1), newImage);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * compute_denoised_pixel_seq
     *
     * Returns the denoised value of a pixel
     *
     * Parameters
     *   padded_input    - array containing a padded single color channel of image data
     *   x               - X coordinate of the pixel in padded_input to denoise
     *   y               - Y coordinate of the pixel in padded_input to denoise
     *   sim_window_1    - pointer to similarity window 1
     *   sim_window_2    - pointer to similarity window 2
     *   radio_search    - radio of search window (t)
     *   radio_sim       - radio of similarity window (f)
     *   degree          - degree of filtering (h)
     *   kernel          - kernel to assist in properly weighting noise values
     */
    static float compute_denoised_pixel_seq(float[] padded_input, int x, int y,
                                       FloatImage image, float[] sim_window_1,
                                       int radio_search, int radio_sim, int degree,
                                       float[] kernel) {
        int original_width = image.getWidth();
        int original_height = image.getHeight();

        // Initialize search window bounds for this pixel
        // DUPLICATE CALCULATIONS (these could be passed in and reused)
        int r_min = ((x-radio_search) > radio_sim) ? (x-radio_search) : radio_sim;
        int r_max = ((x+radio_search) < (radio_sim+original_width-1)) ? (x+radio_search) : (radio_sim+original_width-1);
        int s_min = ((y-radio_search) > radio_sim) ? (y-radio_search) : radio_sim;
        int s_max = ((y+radio_search) < (radio_sim+original_height-1)) ? (y+radio_search) : (radio_sim+original_height-1);
        float average = 0;
        float s_weight = 0;
        float w_max = 0;

        // Some duplicate calculations that could be removed later
        int padded_input_width = 2 * radio_sim + original_width;
        int sim_diam = 2 * radio_sim + 1;
        int sim_size = sim_diam * sim_diam;

        // Compare similarity of every pixel in simlarity window
        // printf("\n\n (I,J) = (%d, %d)\n", x, y);
        for(int s = s_min; s < s_max; s++) {
            for(int r = r_min; r < r_max; r++) {

                if(r == x && s == y) continue;   // Skip checking this pixel against itself

                // Populate second similarity window
                float[] sim_window_2 = populate_sim_window_seq(radio_sim, padded_input, padded_input_width, r, s);

                // Calculate sum d
                float sum = 0;
                for(int i = 0; i < sim_size; i++) {
                    float element = (sim_window_1[i] - sim_window_2[i]);
                    float product = kernel[i] * (float)Math.pow((double)element,2.0);
                    sum = sum + product;
                }

                // Calculate W and determine if it's the new max
                float w = (float)Math.exp( -sum / degree );
                if(w > w_max)
                    w_max = w;

                // printf(" - (%d, %d) sum: %5.4f, w: %5.4f, wmax: %5.4f\n", r, s, sum, w, wmax);

                // Update sweight and average for this similarity window pixel
                s_weight = s_weight + w;
                average = average + (w * padded_input[s * padded_input_width + r]);
            }
        }

        // Update average and sweight for this pixel
        average = average + (w_max * padded_input[y * padded_input_width + x]);
        s_weight = s_weight + w_max;

        // Determine noise value for current pixel (if s_weight > 0)
        if(s_weight > 0)
            return average / s_weight;
        else
            return padded_input[y * padded_input_width + x];
    }

    private static float[] generate_padded_color_array(FloatImage image, int channel, int pad_x, int pad_y, float pad_value) {

        int height = image.getHeight(), width = image.getWidth();
        int new_height = 2*pad_y + height;
        int new_width = 2* pad_x + width;
        float[] paddedImage = new float[new_height*new_width];
        for (int i=0;i< new_width; i++) {
            for (int j=0;j<new_height; j++) {
                if ((i >= pad_x) && (i < (width + pad_x)) && (j >= pad_y) && (j < (height + pad_y))) {
                    paddedImage[j * new_width + i] = image.getPixel(i-pad_x, j-pad_y, channel);
                }
                else {
                    paddedImage[j * new_width + i] = pad_value;
                }
            }
        }
        return paddedImage;
    }

    static float[] generate_kernel_seq(int f){

        //Variable definitions
        float[] kernel= new float[(2*f+1)*(2*f+1)];
        int i, j;

        //Allocate memory for kernel
        int kernel_diam = (2*f + 1);
        int kernel_size = ((2*f + 1) * (2*f + 1));

        // Populate kernel values
        for(int dist = 0; dist < f; dist++) {
            // Calculate weight for this distance
            float value = 1 / (float) Math.pow((double)(2 * (dist+1) + 1), 2);

            // Add this value to all cells within 'dist' of center of kernel
            for(j = 0-(dist+1); j <= (dist+1); j++) {
                for(i = 0-(dist+1); i <= (dist+1); i++) {
                    int index = (f-j) * kernel_diam + (f-i);
                    kernel[index] = kernel[index] + value;
                }
            }
        }

        // Divide all values by f and calculate sum
        float sum = 0;
        for(i = 0; i < kernel_size; i++) {
            kernel[i] = kernel[i] / f;
            sum = sum + kernel[i];
        }

        // Normalize kernel
        for(i = 0; i < kernel_size; i++) {
            kernel[i] = kernel[i] / sum;
        }

        return kernel;
    }


    /**
     * populate_sim_window_seq
     *
     * Copies pixels from the input array into the similarity window array
     *
     * Parameters
     *   sim_window          - similarity window target array
     *   radio_sim           - radius of the similarity window
     *   padded_input        - array from which to copy values
     *   padded_input_width  - width of the input array
     *   pole_x              - X coordinate of the pixel in the input array that should
     *                         be the center pixel of the similarity window
     *   pole_y              - Y coordinate of the pixel in the input array that should
     *                         be the center pixel of the similarity window
     */
    static float[] populate_sim_window_seq(int radio_sim,
                                 float[] padded_input, int padded_input_width, int pole_x, int pole_y) {

        // Calculate dimensions of similarity window
        int sim_diam = 2*radio_sim + 1;  // DUPLICATE CALCULATION
        float[] sim_window = new float[sim_diam * sim_diam];

        // Copy values from input array to similarity window
        for(int i = 0; i < sim_diam; i++) {
            for(int j = 0; j < sim_diam; j++) {
                int index = ((j + pole_y - radio_sim) * padded_input_width) + (i + pole_x - radio_sim);
                sim_window[j * sim_diam + i] = padded_input[index];
            }
        }
        return sim_window;
    }

}








