package net.certiv.tools.indentguide.adaptors;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeSettings;
import org.eclipse.core.runtime.preferences.IScopeContext;

import net.certiv.tools.indentguide.util.Utils;

public class ContentTypeAdaptor implements IContentType {

	private static final String ID_UNK = Utils.UNKNOWN;
	private static final String NAME_UNK = "Unknown/Undefined Content Type";

	private final String id;
	private final String name;
	private final IContentType baseType;
	private final String baseTypeId;

	public static IContentType unknown(IContentType baseType) {
		return new ContentTypeAdaptor(ID_UNK, NAME_UNK, baseType);
	}

	public ContentTypeAdaptor(String uniqueId, String name, IContentType baseType) {
		this.id = uniqueId;
		this.name = name;
		this.baseType = baseType;
		this.baseTypeId = baseType.getId();
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	public String getBaseId() {
		return baseTypeId;
	}

	@Override
	public IContentType getBaseType() {
		return baseType;
	}

	@Override
	public boolean isKindOf(IContentType type) {
		if (type == null) return false;
		if (this == type) return true;
		return baseType != null && baseType.isKindOf(type);
	}

	@Override
	public boolean isUserDefined() {
		return false;
	}

	@Override
	public String toString() {
		return id;
	}

	// ----------------------------------------------------

	@Override
	public void addFileSpec(String fileSpec, int type) throws CoreException {}

	@Override
	public void removeFileSpec(String fileSpec, int type) throws CoreException {}

	@Override
	public void setDefaultCharset(String userCharset) throws CoreException {}

	@Override
	public IContentDescription getDefaultDescription() {
		return null;
	}

	@Override
	public IContentDescription getDescriptionFor(InputStream contents, QualifiedName[] options) throws IOException {
		return null;
	}

	@Override
	public IContentDescription getDescriptionFor(Reader contents, QualifiedName[] options) throws IOException {
		return null;
	}

	@Override
	public String getDefaultCharset() {
		return null;
	}

	@Override
	public String[] getFileSpecs(int type) {
		return null;
	}

	@Override
	public boolean isAssociatedWith(String fileName) {
		return false;
	}

	@Override
	public boolean isAssociatedWith(String fileName, IScopeContext context) {
		return false;
	}

	@Override
	public IContentTypeSettings getSettings(IScopeContext context) throws CoreException {
		return null;
	}
}
