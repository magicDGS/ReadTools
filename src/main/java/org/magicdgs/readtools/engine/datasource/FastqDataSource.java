package org.magicdgs.readtools.engine.datasource;

import org.magicdgs.readtools.utils.iterators.FastqToReadIterator;
import org.magicdgs.readtools.utils.iterators.paired.GATKReadPairedIterator;
import org.magicdgs.readtools.utils.read.writer.ReadToolsIOFormat;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.fastq.FastqReader;
import htsjdk.samtools.util.FastqQualityFormat;
import htsjdk.samtools.util.QualityEncodingDetector;
import org.broadinstitute.hellbender.utils.io.IOUtils;
import org.broadinstitute.hellbender.utils.read.GATKRead;
import scala.Tuple2;

import java.nio.file.Path;
import java.util.Iterator;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class FastqDataSource extends RTDataSource {

    // add constructor params
    private final Path fastqPath;
    private final boolean interleaved;
    private final SAMFileHeader header;
    private final FastqQualityFormat format;
    private final FastqReader reader;

    public FastqDataSource(final String source, final boolean interleaved) {
        this.fastqPath = IOUtils.getPath(source);
        this.interleaved = interleaved;
        // TODO: set group order by read name
        this.header = new SAMFileHeader();
        // TODO: close writers
        this.format = QualityEncodingDetector.detect(readerFactory.openFastqReader(fastqPath));
        this.reader = readerFactory.openFastqReader(fastqPath);
    }

    @Override
    public boolean isPaired() {
        return interleaved;
    }

    @Override
    public FastqQualityFormat sourceEncoding() {
        return format;
    }

    @Override
    public SAMFileHeader getHeader() {
        return header;
    }

    @Override
    public boolean isSource(final String source) {
        return ReadToolsIOFormat.isFastq(source);
    }

    @Override
    protected Iterator<GATKRead> sourceIterator() {
        return new FastqToReadIterator(reader.iterator());
    }

    @Override
    protected Iterator<Tuple2<GATKRead, GATKRead>> sourcePairedIterator() {
        if (!isPaired()) {
            throw new IllegalStateException("TODO: msg");
        }
        return GATKReadPairedIterator.of(sourceIterator());
    }

    @Override
    public void close() throws Exception {
        reader.close();
    }
}
