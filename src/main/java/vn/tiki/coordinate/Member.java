package vn.tiki.coordinate;

import io.gridgo.bean.BArray;
import io.gridgo.bean.BElement;
import io.gridgo.bean.BObject;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import vn.tiki.coordinate.MemberImpl.MemberBuilder;

public interface Member {

	String getId();

	BObject getData();

	byte[] toBytes();

	static MemberBuilder newBuilder() {
		return MemberImpl.builder();
	}

	static Member fromBytes(byte[] bytes) {
		BArray barray = BElement.ofBytes(bytes);
		return MemberImpl.builder()//
				.id(barray.removeString(0)) //
				.data(barray.removeObject(0)) //
				.build();
	}
}

@Getter
@Builder(builderClassName = "MemberBuilder")
class MemberImpl implements Member {

	private @NonNull String id;

	@Builder.Default
	private BObject data = BObject.ofEmpty();

	private BArray serialize() {
		BArray result = BArray.ofEmpty() //
				.addAny(this.getId()) //
				.addAny(this.getData()) //
		;
		return result;
	}

	public byte[] toBytes() {
		return this.serialize().toBytes();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Member) {
			return ((Member) obj).getId().equals(this.getId());
		}
		return false;
	}

	@Override
	public String toString() {
		return this.getId() + " -> data: " + this.getData();
	}
}
