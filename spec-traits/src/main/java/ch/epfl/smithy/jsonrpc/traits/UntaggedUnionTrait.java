package ch.epfl.smithy.jsonrpc.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AnnotationTrait;
import software.amazon.smithy.model.traits.AbstractTrait;

public final class UntaggedUnionTrait extends AnnotationTrait {
	public static ShapeId ID = ShapeId.from("jsonrpc#untaggedUnion");

	public UntaggedUnionTrait() {
		super(ID, Node.objectNode());
	}

	public static final class Provider extends AnnotationTrait.Provider<UntaggedUnionTrait> {
		public Provider() {
			super(ID, (node) -> new UntaggedUnionTrait());
		}
	}
}