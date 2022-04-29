package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import static edu.harvard.iq.dataverse.dataaccess.DataAccess.getStorageIO;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.FileUtil;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import org.apache.commons.io.IOUtils;
import static edu.harvard.iq.dataverse.dataaccess.DataAccess.getStorageIO;
import edu.harvard.iq.dataverse.datasetutility.FileSizeChecker;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.license.License;
import edu.harvard.iq.dataverse.util.StringUtil;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;
import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;

import org.apache.commons.io.FileUtils;

public class DatasetUtil {

    private static final Logger logger = Logger.getLogger(DatasetUtil.class.getCanonicalName());
    public static String datasetLogoFilenameFinal = "dataset_logo_original";
    public static String datasetLogoThumbnail = "dataset_logo";
    public static String thumbExtension = ".thumb";

    public static List<DatasetThumbnail> getThumbnailCandidates(Dataset dataset, boolean considerDatasetLogoAsCandidate, int size) {
        List<DatasetThumbnail> thumbnails = new ArrayList<>();
        if (dataset == null) {
            return thumbnails;
        }
        if (considerDatasetLogoAsCandidate) {
//            Path path = Paths.get(dataset.getFileSystemDirectory() + File.separator + datasetLogoThumbnail + thumb48addedByImageThumbConverter);
//            if (Files.exists(path)) {
//                logger.fine("Thumbnail created from dataset logo exists!");
//                File file = path.toFile();
//                try {
//                    byte[] bytes = Files.readAllBytes(file.toPath());
            StorageIO<Dataset> dataAccess = null;

            try{
                dataAccess = DataAccess.getStorageIO(dataset);
            }
            catch(IOException ioex){
            }

            InputStream in = null;
            try {
                    in = dataAccess.getAuxFileAsInputStream(datasetLogoThumbnail + thumbExtension + size);
            } catch (Exception ioex) {
            }

            if (in != null) {
                logger.fine("Thumbnail created from dataset logo exists!");
                try {
                    byte[] bytes = IOUtils.toByteArray(in);
                    String base64image = Base64.getEncoder().encodeToString(bytes);
                    DatasetThumbnail datasetThumbnail = new DatasetThumbnail(FileUtil.DATA_URI_SCHEME + base64image, null);
                    thumbnails.add(datasetThumbnail);
                } catch (IOException ex) {
                    logger.warning("Unable to rescale image: " + ex);
                }
            } else {
                logger.fine("There is no thumbnail created from a dataset logo");
            }
	    IOUtils.closeQuietly(in);
        }
        for (FileMetadata fileMetadata : dataset.getLatestVersion().getFileMetadatas()) {
            DataFile dataFile = fileMetadata.getDataFile();

            if (dataFile != null && FileUtil.isThumbnailSupported(dataFile)
                    && ImageThumbConverter.isThumbnailAvailable(dataFile)
                    && !dataFile.isRestricted()
                    && !FileUtil.isActivelyEmbargoed(dataFile)) {
                String imageSourceBase64 = null;
                imageSourceBase64 = ImageThumbConverter.getImageThumbnailAsBase64(dataFile, size);

                if (imageSourceBase64 != null) {
                    DatasetThumbnail datasetThumbnail = new DatasetThumbnail(imageSourceBase64, dataFile);
                    thumbnails.add(datasetThumbnail);
                }
            }
        }
        return thumbnails;
    }

    /**
     * Note "datasetVersionId" can be null. If needed, it helps the "efficiency"
     * of "attemptToAutomaticallySelectThumbnailFromDataFiles"
     *
     * @param dataset
     * @param datasetVersion
     * @return
     */
    public static DatasetThumbnail getThumbnail(Dataset dataset, DatasetVersion datasetVersion, int size) {
        if (dataset == null) {
            return null;
        }

        StorageIO<Dataset> dataAccess = null;
                
        try{
            dataAccess = DataAccess.getStorageIO(dataset);
        }
        catch(IOException ioex){
            logger.warning("getThumbnail(): Failed to initialize dataset StorageIO for " + dataset.getStorageIdentifier() + " (" + ioex.getMessage() + ")");
        }
        
        InputStream in = null;
        try {
            if (dataAccess == null) {
                logger.warning("getThumbnail(): Failed to initialize dataset StorageIO for " + dataset.getStorageIdentifier());
            } else {
                in = dataAccess.getAuxFileAsInputStream(datasetLogoThumbnail + ".thumb" + size);
            }
        } catch (IOException ex) {
            in = null; 
            logger.fine("Dataset-level thumbnail file does not exist, or failed to open; will try to find an image file that can be used as the thumbnail.");
        }

        
        if (in != null) {
            try {
                byte[] bytes = IOUtils.toByteArray(in);
                String base64image = Base64.getEncoder().encodeToString(bytes);
                DatasetThumbnail datasetThumbnail = new DatasetThumbnail(FileUtil.DATA_URI_SCHEME + base64image, null);
                logger.fine("will get thumbnail from dataset logo");
                return datasetThumbnail;
            } catch (IOException ex) {
                logger.fine("Unable to read thumbnail image from file: " + ex);
                return null;
            } finally
	    {
		    IOUtils.closeQuietly(in);
	    }
        } else {
            DataFile thumbnailFile = dataset.getThumbnailFile();

            if (thumbnailFile !=null && (thumbnailFile.isRestricted() || FileUtil.isActivelyEmbargoed(thumbnailFile))) {
                logger.fine("Dataset (id :" + dataset.getId() + ") has a thumbnail (user selected or automatically chosen) but the file must have later been restricted or embargoed. Returning null.");
                thumbnailFile= null;
            }
            if (thumbnailFile == null) {
                if (dataset.isUseGenericThumbnail()) {
                    logger.fine("Dataset (id :" + dataset.getId() + ") does not have a thumbnail and is 'Use Generic'.");
                    return null;
                } else {
                    thumbnailFile = attemptToAutomaticallySelectThumbnailFromDataFiles(dataset, datasetVersion);
                    if (thumbnailFile == null) {
                        logger.fine("Dataset (id :" + dataset.getId() + ") does not have a thumbnail available that could be selected automatically.");
                        return null;
                    } else {
                        String imageSourceBase64 = ImageThumbConverter.getImageThumbnailAsBase64(thumbnailFile, size);
                        DatasetThumbnail defaultDatasetThumbnail = new DatasetThumbnail(imageSourceBase64, thumbnailFile);
                        logger.fine("thumbnailFile (id :" + thumbnailFile.getId() + ") will get thumbnail through automatic selection from DataFile id " + thumbnailFile.getId());
                        return defaultDatasetThumbnail;
                    }
                }
            } else {
                String imageSourceBase64 = ImageThumbConverter.getImageThumbnailAsBase64(thumbnailFile, size);
                DatasetThumbnail userSpecifiedDatasetThumbnail = new DatasetThumbnail(imageSourceBase64, thumbnailFile);
                logger.fine("Dataset (id :" + dataset.getId() + ")  will get thumbnail the user specified from DataFile id " + thumbnailFile.getId());
                return userSpecifiedDatasetThumbnail;

            }
        }
    }

    public static DatasetThumbnail getThumbnail(Dataset dataset, int size) {
        if (dataset == null) {
            return null;
        }
        return getThumbnail(dataset, null, size);
    }

    public static boolean deleteDatasetLogo(Dataset dataset) {
        if (dataset == null) {
            return false;
        }
        try {
            StorageIO<Dataset> storageIO = getStorageIO(dataset);

            if (storageIO == null) {
                logger.warning("Null storageIO in deleteDatasetLogo()");
                return false;
            }

            storageIO.deleteAuxObject(datasetLogoFilenameFinal);
            storageIO.deleteAuxObject(datasetLogoThumbnail + thumbExtension + ImageThumbConverter.DEFAULT_DATASETLOGO_SIZE);
            storageIO.deleteAuxObject(datasetLogoThumbnail + thumbExtension + ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE);

        } catch (IOException ex) {
            logger.info("Failed to delete dataset logo: " + ex.getMessage());
            return false;
        }
        return true;
                
        //TODO: Is this required? 
//        File originalFile = new File(dataset.getFileSystemDirectory().toString(), datasetLogoFilenameFinal);
//        boolean originalFileDeleted = originalFile.delete();
//        File thumb48 = new File(dataset.getFileSystemDirectory().toString(), File.separator + datasetLogoThumbnail + thumb48addedByImageThumbConverter);
//        boolean thumb48Deleted = thumb48.delete();
//        if (originalFileDeleted && thumb48Deleted) {
//            return true;
//        } else {
//            logger.info("One of the files wasn't deleted. Original deleted: " + originalFileDeleted + ". thumb48 deleted: " + thumb48Deleted + ".");
//            return false;
//        }
    }

    /**
     * Pass an optional datasetVersion in case the file system is checked
     *
     * @param dataset
     * @param datasetVersion
     * @return
     */
    public static DataFile attemptToAutomaticallySelectThumbnailFromDataFiles(Dataset dataset, DatasetVersion datasetVersion) {
        if (dataset == null) {
            return null;
        }

        if (dataset.isUseGenericThumbnail()) {
            logger.fine("Bypassing logic to find a thumbnail because a generic icon for the dataset is desired.");
            return null;
        }

        if (datasetVersion == null) {
            logger.fine("getting a published version of the dataset");
            // We want to use published files only when automatically selecting 
            // dataset thumbnails.
            datasetVersion = dataset.getReleasedVersion(); 
        }
        
        // No published version? - No [auto-selected] thumbnail for you.
        if (datasetVersion == null) {
            return null; 
        }

        for (FileMetadata fmd : datasetVersion.getFileMetadatas()) {
            DataFile testFile = fmd.getDataFile();
            // We don't want to use a restricted image file as the dedicated thumbnail:
            if (!testFile.isRestricted() && !FileUtil.isActivelyEmbargoed(testFile) && FileUtil.isThumbnailSupported(testFile) && ImageThumbConverter.isThumbnailAvailable(testFile, ImageThumbConverter.DEFAULT_DATASETLOGO_SIZE)) {
                return testFile;
            }
        }
        logger.fine("In attemptToAutomaticallySelectThumbnailFromDataFiles and interated through all the files but couldn't find a thumbnail.");
        return null;
    }

    public static Dataset persistDatasetLogoToStorageAndCreateThumbnails(Dataset dataset, InputStream inputStream) {
        if (dataset == null) {
            return null;
        }
        File tmpFile = null;
        try {
            tmpFile = FileUtil.inputStreamToFile(inputStream);
        } catch (IOException ex) {
        	logger.severe(ex.getMessage());
        }

        StorageIO<Dataset> dataAccess = null;
                
        try{
             dataAccess = DataAccess.getStorageIO(dataset);
        }
        catch(IOException ioex){
            //TODO: Add a suitable waing message
            logger.warning("Failed to save the file, storage id " + dataset.getStorageIdentifier() + " (" + ioex.getMessage() + ")");
        }
        
        //File originalFile = new File(datasetDirectory.toString(), datasetLogoFilenameFinal);
        try {
            //this goes through Swift API/local storage/s3 to write the dataset thumbnail into a container
            dataAccess.savePathAsAux(tmpFile.toPath(), datasetLogoFilenameFinal);
        } catch (IOException ex) {
            logger.severe("Failed to move original file from " + tmpFile.getAbsolutePath() + " to its DataAccess location" + ": " + ex);
        }

        BufferedImage fullSizeImage = null;
        try {
            fullSizeImage = ImageIO.read(tmpFile);
        } catch (IOException ex) {
        	IOUtils.closeQuietly(inputStream);
            logger.severe(ex.getMessage());
            return null;
        }
        if (fullSizeImage == null) {
            logger.fine("fullSizeImage was null!");
            IOUtils.closeQuietly(inputStream);
            return null;
        }
        int width = fullSizeImage.getWidth();
        int height = fullSizeImage.getHeight();
        FileChannel src = null;
        try {
            src = new FileInputStream(tmpFile).getChannel();
        } catch (FileNotFoundException ex) {
        	IOUtils.closeQuietly(inputStream);
            logger.severe(ex.getMessage());
            return null;
        }
        FileChannel dest = null;
        try {
            dest = new FileOutputStream(tmpFile).getChannel();
        } catch (FileNotFoundException ex) {
        	IOUtils.closeQuietly(inputStream);
            logger.severe(ex.getMessage());
            return null;
        }
        try {
            dest.transferFrom(src, 0, src.size());
        } catch (IOException ex) {
            logger.severe(ex.getMessage());
            return null;
        }
        File tmpFileForResize = null;
        try {
        	//The stream was used around line 274 above, so this creates an empty file (OK since all it is used for is getting a path, but not reusing it here would make it easier to close it above.)
            tmpFileForResize = FileUtil.inputStreamToFile(inputStream);
        } catch (IOException ex) {
            logger.severe(ex.getMessage());
            return null;
        } finally {
        	IOUtils.closeQuietly(inputStream);
        }
        // We'll try to pre-generate the rescaled versions in both the 
        // DEFAULT_DATASET_LOGO (currently 140) and DEFAULT_CARDIMAGE_SIZE (48)
        String thumbFileLocation = ImageThumbConverter.rescaleImage(fullSizeImage, width, height, ImageThumbConverter.DEFAULT_DATASETLOGO_SIZE, tmpFileForResize.toPath().toString());
        logger.fine("thumbFileLocation = " + thumbFileLocation);
        logger.fine("tmpFileLocation=" + tmpFileForResize.toPath().toString());
        //now we must save the updated thumbnail 
        try {
            dataAccess.savePathAsAux(Paths.get(thumbFileLocation), datasetLogoThumbnail+thumbExtension+ImageThumbConverter.DEFAULT_DATASETLOGO_SIZE);
        } catch (IOException ex) {
            logger.severe("Failed to move updated thumbnail file from " + tmpFile.getAbsolutePath() + " to its DataAccess location" + ": " + ex);
        }
        
        thumbFileLocation = ImageThumbConverter.rescaleImage(fullSizeImage, width, height, ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE, tmpFileForResize.toPath().toString());
        logger.fine("thumbFileLocation = " + thumbFileLocation);
        logger.fine("tmpFileLocation=" + tmpFileForResize.toPath().toString());
        //now we must save the updated thumbnail 
        try {
            dataAccess.savePathAsAux(Paths.get(thumbFileLocation), datasetLogoThumbnail+thumbExtension+ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE);
        } catch (IOException ex) {
            logger.severe("Failed to move updated thumbnail file from " + tmpFile.getAbsolutePath() + " to its DataAccess location" + ": " + ex);
        }
        
        //This deletes the tempfiles created for rescaling and encoding
        boolean tmpFileWasDeleted = tmpFile.delete();
        boolean originalTempFileWasDeleted = tmpFileForResize.delete();
        try {
            Files.delete(Paths.get(thumbFileLocation));
        } catch (IOException ioex) {
            logger.fine("Failed to delete temporary thumbnail file");
        }
        
        logger.fine("Thumbnail saved to " + thumbFileLocation + ". Temporary file deleted : " + tmpFileWasDeleted + ". Original file deleted : " + originalTempFileWasDeleted);
        return dataset;
    }

    public static InputStream getThumbnailAsInputStream(Dataset dataset, int size) {
        if (dataset == null) {
            return null;
        }
        DatasetThumbnail datasetThumbnail = dataset.getDatasetThumbnail(size);
        if (datasetThumbnail == null) {
            return null;
        } else {
            String base64Image = datasetThumbnail.getBase64image();
            String leadingStringToRemove = FileUtil.DATA_URI_SCHEME;
            String encodedImg = base64Image.substring(leadingStringToRemove.length());
            byte[] decodedImg = null;
            try {
                decodedImg = Base64.getDecoder().decode(encodedImg.getBytes("UTF-8"));
                logger.fine("returning this many bytes for  " + "dataset id: " + dataset.getId() + ", persistentId: " + dataset.getIdentifier() + " :" + decodedImg.length);
            } catch (UnsupportedEncodingException ex) {
                logger.info("dataset thumbnail could not be decoded for dataset id " + dataset.getId() + ": " + ex);
                return null;
            }
            ByteArrayInputStream nonDefaultDatasetThumbnail = new ByteArrayInputStream(decodedImg);
            logger.fine("For dataset id " + dataset.getId() + " a thumbnail was found and is being returned.");
            return nonDefaultDatasetThumbnail;
        }
    }

    /**
     * The dataset logo is the file that a user uploads which is *not* one of
     * the data files. Compare to the datavese logo. We do not save the original
     * file that is uploaded. Rather, we delete it after first creating at least
     * one thumbnail from it. 
     */
    public static boolean isDatasetLogoPresent(Dataset dataset, int size) {
        if (dataset == null) {
            return false;
        }

        StorageIO<Dataset> dataAccess = null;

        try {
            dataAccess = DataAccess.getStorageIO(dataset);
            return dataAccess.isAuxObjectCached(datasetLogoThumbnail + thumbExtension + size);
        } catch (IOException ioex) {
        }
        return false;
    }

    public static List<DatasetField> getDatasetSummaryFields(DatasetVersion datasetVersion, String customFields) {
        
        List<DatasetField> datasetFields = new ArrayList<>();
        
        //if customFields are empty, go with default fields. 
        if(customFields==null || customFields.isEmpty()){
               customFields="dsDescription,subject,keyword,publication,notesText";
        }
        
        String[] customFieldList= customFields.split(",");
        Map<String,DatasetField> DatasetFieldsSet=new HashMap<>(); 
        
        for (DatasetField dsf : datasetVersion.getFlatDatasetFields()) {
            DatasetFieldsSet.put(dsf.getDatasetFieldType().getName(),dsf); 
        }
        
        for(String cfl : customFieldList)
        {
                DatasetField df = DatasetFieldsSet.get(cfl);
                if(df!=null)
                datasetFields.add(df);
        }
            
        return datasetFields;
    }
    
    public static boolean isAppropriateStorageDriver(Dataset dataset){
        // ToDo - rsync was written before multiple store support and currently is hardcoded to use the DataAccess.S3 store. 
        // When those restrictions are lifted/rsync can be configured per store, this test should check that setting
        // instead of testing for the 's3" store,
        //This method is used by both the dataset and edit files page so one change here
        //will fix both
       return dataset.getEffectiveStorageDriverId().equals(DataAccess.S3);
    }
    
    /**
     * Given a dataset version, return it's size in human readable units such as
     * 42.9 MB.There is a GetDatasetStorageSizeCommand but it's overly complex
     * for the use case.
     *
     * @param original Use the original file size rather than the archival file
     * size for tabular files.
     */
    public static String getDownloadSize(DatasetVersion dsv, boolean original) {
        return FileSizeChecker.bytesToHumanReadable(getDownloadSizeNumeric(dsv, original));
    }
    
    public static Long getDownloadSizeNumeric(DatasetVersion dsv, boolean original) {
        return getDownloadSizeNumericBySelectedFiles(dsv.getFileMetadatas(), original);
    }
    
    public static Long getDownloadSizeNumericBySelectedFiles(List<FileMetadata> fileMetadatas, boolean original) {
        long bytes = 0l;
        for (FileMetadata fileMetadata : fileMetadatas) {
            DataFile dataFile = fileMetadata.getDataFile();
            if (original && dataFile.isTabularData()) {                
                bytes += dataFile.getOriginalFileSize() == null ? 0 : dataFile.getOriginalFileSize();
            } else {
                bytes += dataFile.getFilesize();
            }
        }
        return bytes;
    }
    
    public static boolean validateDatasetMetadataExternally(Dataset ds, String executable, DataverseRequest request) {
        String sourceAddressLabel = "0.0.0.0"; 
        
        if (request != null) {
            IpAddress sourceAddress = request.getSourceAddress();
            if (sourceAddress != null) {
                sourceAddressLabel = sourceAddress.toString();
            }
        }
        
        String jsonMetadata; 
        
        try {
            jsonMetadata = json(ds).add("datasetVersion", json(ds.getLatestVersion())).add("sourceAddress", sourceAddressLabel).build().toString();
        } catch (Exception ex) {
            logger.warning("Failed to export dataset metadata as json; "+ex.getMessage() == null ? "" : ex.getMessage());
            return false; 
        }
        
        if (StringUtil.isEmpty(jsonMetadata)) {
            logger.warning("Failed to export dataset metadata as json.");
            return false; 
        }
       
        // save the metadata in a temp file: 
        
        try {
            File tempFile = File.createTempFile("datasetMetadataCheck", ".tmp");
            FileUtils.writeStringToFile(tempFile, jsonMetadata);
                                    
            // run the external executable: 
            String[] params = { executable, tempFile.getAbsolutePath() };
            Process p = Runtime.getRuntime().exec(params);
            p.waitFor(); 
            
            return p.exitValue() == 0;
 
        } catch (IOException | InterruptedException ex) {
            logger.warning("Failed run the external executable.");
            return false; 
        }
        
    }

    public static String getLicenseName(DatasetVersion dsv) {
        License license = dsv.getTermsOfUseAndAccess().getLicense();
        return license != null ? license.getName()
                : BundleUtil.getStringFromBundle("license.custom");
    }

    public static String getLicenseURI(DatasetVersion dsv) {
        License license = dsv.getTermsOfUseAndAccess().getLicense();
        // Return the URI
        // For standard licenses, just return the stored URI
        return (license != null) ? license.getUri().toString()
                // For custom terms, construct a URI with :draft or the version number in the URI
                : (dsv.getVersionState().name().equals("DRAFT")
                        ? dsv.getDataverseSiteUrl()
                                + "/api/datasets/:persistentId/versions/:draft/customlicense?persistentId="
                                + dsv.getDataset().getGlobalId().asString()
                        : dsv.getDataverseSiteUrl() + "/api/datasets/:persistentId/versions/" + dsv.getVersionNumber()
                                + "." + dsv.getMinorVersionNumber() + "/customlicense?persistentId="
                                + dsv.getDataset().getGlobalId().asString());
    }

    public static String getLicenseIcon(DatasetVersion dsv) {
        License license = dsv.getTermsOfUseAndAccess().getLicense();
        return license != null && license.getIconUrl() != null ? license.getIconUrl().toString() : null;
    }

    public static String getLicenseDescription(DatasetVersion dsv) {
        License license = dsv.getTermsOfUseAndAccess().getLicense();
        return license != null ? license.getShortDescription() : BundleUtil.getStringFromBundle("license.custom.description");
    }

    public static String getLocaleExternalStatus(String status) {
        String localizedName =  "" ;
        try {
            localizedName = BundleUtil.getStringFromPropertyFile(status.toLowerCase().replace(" ", "_"), "CurationLabels");
        }
        catch (Exception e) {
            localizedName = status;
        }

        if (localizedName == null) {
            localizedName = status ;
        }
        return localizedName;
    }
}
