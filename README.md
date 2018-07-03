# Basic Serialization
## Serialization
`Serializer` - interface for serialization to valid json values
`DefaultSerializer` - default implementation of `Serializer`

## Deserialization 
`Deserializer` - interface for deserialization of strings
`DefaultDeserializer` - default implementation of `Deserializer`, can deserialize enums,
strings, integers and floats.

# CSV Serialization
## Streaming
`StreamSerializable` is an interface for classes that can be serialized to a stream, given a `Serializer`.
## DataTable
`DataTable<T>` implements `StreamSerializable`. It is constructed from a `Sequence<T>` of data to be written as rows
to a CSV on serialization with column headers derived from the field names of type `T`.
## FlexibleDataTable
`FlexibleDataTable<T>` allows you to serialize a sequence of objects of type `T`, where one of the properties of
type `T` is of type `Map<String,*>` and marked with the `FlexibleProperty` annotation. In this case the keys of the map
are serialized to column headers in the resulting CSV, with their value pairs as cell values. A list of keys of the map 
that the user wants to appear as column headers must be passed explicitly as a constructor argument and any not
included in that argument will be ignored.

# CSV Deserialization
## DataTableDeserializer
`DataTableDeserializer<T>` takes a streamed CSV and returns a sequence of data of type `T`. The column headers of the 
CSV must exactly match up with the field names of `T`, 
and each row in the CSV will be mapped to an object in the sequence. 
## FlexibleDataTableDeserializer
`FlexibleDataTableDeserializer<T>` can deserialize a CSV into a type `T` that has a property of type `Map<String,*>` 
and marked with the `FlexibleProperty` annotation, where some of the CSV headers correspond to keys of the map.
## Usage
The above classes should not usually be instantiated directly. The `DataTableDeserializer.deserialize` method
 takes the target `KClass` as an argument and infers whether or not the `FlexibleDataTableDeserializer` is needed.