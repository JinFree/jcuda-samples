/*
 * JCuda - Java bindings for NVIDIA CUDA
 *
 * Copyright 2008-2016 Marco Hutter - http://www.jcuda.org
 */
package jcuda.samples.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import jcuda.CudaException;

/**
 * Utility methods that are used in the JCuda samples.<br>
 * <br>
 * NOTE: This class is not part of a public API. It is only intended for
 * the use in the samples. Parts of its functionality could be replaced 
 * with the runtime compilation features that have been added in CUDA 7.5. 
 */
public class JCudaSamplesUtils
{
    /**
     * The logger used in this class
     */
    private static final Logger logger =
        Logger.getLogger(JCudaSamplesUtils.class.getName());
    
    /**
     * Compiles the given CUDA file into a PTX file using NVCC, and returns
     * the name of the resulting PTX file
     * 
     * @param cuFileName The CUDA file name
     * @return The PTX file name
     * @throws CudaException If an error occurs - i.e. when the input file
     * does not exist, or the NVCC call caused an error.
     */
    public static String preparePtxFile(String cuFileName)
    {
        return invokeNvcc(cuFileName, "ptx", true);
    }
    
    /**
     * Tries to create a PTX or CUBIN file for the given CUDA file. <br>
     * <br>
     * The extension of the given file name is replaced with 
     * <code>"cubin"</code> or <code>"ptx"</code>, depending on the 
     * <code>targetFileType</code>.<br>
     * <br>
     * If the file with the resulting name does not exist yet, or if 
     * <code>forceRebuild</code> is <code>true</code>, then it is compiled 
     * from the given file using NVCC, using the given parameters.<br>
     * <br>
     * The name of the resulting output file is returned.
     *
     * @param cuFileName The name of the .CU file
     * @param targetFileType The target file type. Must be <code>"cubin"</code>
     * or <code>"ptx"</code> (case-insensitively)
     * @param forceRebuild Whether the PTX file should be created even if
     * it already exists
     * @return The name of the PTX file
     * @throws CudaException If an error occurs - i.e. when the input file
     * does not exist, or the NVCC call caused an error.
     * @throws IllegalArgumentException If the target file type is not valid
     */
    private static String invokeNvcc(
        String cuFileName, String targetFileType, 
        boolean forceRebuild, String ... nvccArguments)
    {
        if (!"cubin".equalsIgnoreCase(targetFileType) &&
            !"ptx".equalsIgnoreCase(targetFileType))
        {
            throw new IllegalArgumentException(
                "Target file type must be \"ptx\" or \"cubin\", but is " + 
                targetFileType);
        }
        logger.info("Creating " + targetFileType + " file for " + cuFileName);
        
        int dotIndex = cuFileName.lastIndexOf('.');
        if (dotIndex == -1)
        {
            dotIndex = cuFileName.length();
        }
        String otuputFileName = cuFileName.substring(0, dotIndex) + 
            "." + targetFileType.toLowerCase();
        File ptxFile = new File(otuputFileName);
        if (ptxFile.exists() && !forceRebuild)
        {
            return otuputFileName;
        }

        File cuFile = new File(cuFileName);
        if (!cuFile.exists())
        {
            throw new CudaException("Input file not found: " + cuFileName + 
                " (" + cuFile.getAbsolutePath() + ")");
        }
        String modelString = "-m" + System.getProperty("sun.arch.data.model");
        String command = "nvcc ";
        command += modelString + " ";
        command += "-" + targetFileType + " ";
        for (String a : nvccArguments)
        {
            command += a + " ";
        }
        command += cuFileName + " -o " + otuputFileName;

        logger.info("Executing\n" + command);
        try
        {
            Process process = Runtime.getRuntime().exec(command);
    
            String errorMessage = 
                new String(toByteArray(process.getErrorStream()));
            String outputMessage =
                new String(toByteArray(process.getInputStream()));
            int exitValue = 0;
            try
            {
                exitValue = process.waitFor();
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                throw new CudaException(
                    "Interrupted while waiting for nvcc output", e);
            }
            if (exitValue != 0)
            {
                logger.severe("nvcc process exitValue " + exitValue);
                logger.severe("errorMessage:\n" + errorMessage);
                logger.severe("outputMessage:\n" + outputMessage);
                throw new CudaException("Could not create " + targetFileType + 
                    " file: " + errorMessage);
            }
        }
        catch (IOException e)
        {
            throw new CudaException("Could not create " + targetFileType + 
                " file", e);
        }

        logger.info("Finished creating " + targetFileType + " file");
        return otuputFileName;
    }

    /**
     * Fully reads the given InputStream and returns it as a byte array
     *
     * @param inputStream The input stream to read
     * @return The byte array containing the data from the input stream
     * @throws IOException If an I/O error occurs
     */
    private static byte[] toByteArray(InputStream inputStream)
        throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte buffer[] = new byte[8192];
        while (true)
        {
            int read = inputStream.read(buffer);
            if (read == -1)
            {
                break;
            }
            baos.write(buffer, 0, read);
        }
        return baos.toByteArray();
    }

}
