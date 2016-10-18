package de.herrlock.manga.index;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.io.ByteStreams;

import de.herrlock.manga.exceptions.MDRuntimeException;
import de.herrlock.manga.util.configuration.IndexerConfiguration;

public final class IndexerMain {
    private static final Logger logger = LogManager.getLogger();

    public static void writeIndex( final IndexerConfiguration conf ) {
        try {
            // the index-directory
            final Path indexDir = Files.createDirectories( Paths.get( "index" ) );

            // write index.html
            logger.debug( "Writing Index" );
            try ( OutputStream out = Files.newOutputStream( indexDir.resolve( "index.html" ) ) ) {
                exportHtmlIndex( out, conf );
            }

            // unzip DataTables
            logger.debug( "Unzipping" );
            try ( ZipInputStream zin = new ZipInputStream( Indexer.class.getResourceAsStream( "DataTables.zip" ) ) ) {
                ZipEntry entry;
                while ( ( entry = zin.getNextEntry() ) != null ) {
                    String entryName = entry.getName();
                    Path entryPath = indexDir.resolve( entryName );
                    if ( entry.isDirectory() ) {
                        Files.createDirectories( entryPath );
                    } else {
                        Path parent = entryPath.getParent();
                        if ( parent == null ) {
                            throw new IllegalStateException();
                        }
                        Files.createDirectories( parent );
                        InputStream limitedStream = ByteStreams.limit( zin, entry.getSize() );
                        try ( OutputStream targetStream = Files.newOutputStream( entryPath ) ) {
                            ByteStreams.copy( limitedStream, targetStream );
                        }
                        // limitedStream must not be closed, that would close the ZipInputStream
                    }
                }
            }
        } catch ( IOException ex ) {
            throw new MDRuntimeException( ex );
        }
    }

    public static void exportHtmlIndex( final OutputStream out, final IndexerConfiguration conf ) {
        Index index = Indexer.createIndex( conf );

        try ( InputStream xmlDocument = Indexer.class.getResourceAsStream( "manga2datatable.xsl" ) ) {
            Source xsl = new StreamSource( xmlDocument );
            Source xml = new JAXBSource( createMarshaller(), index );
            Result target = new StreamResult( out );
            try {
                Transformer transformer = TransformerFactory.newInstance().newTransformer( xsl );
                transformer.transform( xml, target );
            } catch ( TransformerFactoryConfigurationError | TransformerException ex ) {
                throw new MDRuntimeException( ex );
            }
        } catch ( IOException | JAXBException ex ) {
            throw new MDRuntimeException( ex );
        }
    }

    public static byte[] marshallIndex( final IndexerConfiguration conf ) {
        Index index = Indexer.createIndex( conf );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Marshaller marshaller = createMarshaller();
            marshaller.marshal( index, out );
        } catch ( JAXBException e ) {
            throw new MDRuntimeException( e );
        }
        return out.toByteArray();
    }

    private static Marshaller createMarshaller() throws JAXBException {
        Marshaller marshaller = JAXBContext.newInstance( Index.class ).createMarshaller();
        marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, true );
        return marshaller;
    }

    private IndexerMain() {
        // avoid instantiation
    }

}
