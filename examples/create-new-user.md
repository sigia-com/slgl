# Create a new user

Users in SLGL are created and configured by creating and linking nodes of specific types. These types are SLGL builtin types.

First there is the "user" type, and it is defined as follows:

```json
{
  "@id": "313aa25f-65db-45e9-bb52-74f71ee13ff2",
  "@type": "ce6b07da-96d5-4c09-8659-e9e806dfe1c1",
  "anchors": [
    {
      "id": "#keys",
      "type": "3588ea96-6a11-4e76-aa89-3da79a5298e5"
    },
    {
      "id": "#deletion",
      "type": "1446deae-13fb-4fe9-a751-48d0d3ebdfa7"
    },
    {
      "id": "#credits",
      "type": "af7f8403-b442-412a-8891-b0bb6901ed4e"
    }
  ],
  "permissions": [
    {
      "allow": [
        {
          "action": "all"
        }
      ],
      "require": {
        "$principals": {
          "op": "at_least_one_meets_requirements",
          "value": {
            "$current.api.username": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
          }
        }
      }
    }
  ]
}
```  

There are several important things in that definition:
- `@id` - this is an identifier of that node, but it also acts as an identifier of that type
- `@type` - this is an identifier of "type" type - a type used to define new types
- `anchors` - this is a list of anchors that can be used to link other nodes to a user
  - `#keys` - this anchor is used to configure api keys - each "api key" node linked to user node will be valid api key that can be used be that user to make requests 
  - `#credits` - this anchor is used to add credits - each request costs some credits to make, more complex requests (that takes more time and storage to process) cost more credits
  - `#deletion` - this anchor is used to mark a user as deleted - linking node to this anchor will prevent the user from making any requests
- `permissions` - this defines permissions of user node - in short, all operations on a user (like changing his api key or adding more credit) can only be done by an admin (user with an id `aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa`)

To create a fully functional user you need to make three things:
- create node that will represent a user (node with "user" type: `313aa25f-65db-45e9-bb52-74f71ee13ff2`)
- create node that will represent api key (node with "api key" type: `3588ea96-6a11-4e76-aa89-3da79a5298e5`) and link that node to anchor `#keys` of user's node
- create node that will represent credits (node with "credits" type: `af7f8403-b442-412a-8891-b0bb6901ed4e`) and link that node to anchor `#credits` of user's node

All these nodes (user node, api key node and credit node) together with required links between them can be created in a single request:
```bash
curl <API_URL> -u <USERNAME>:<API_KEY> -H 'Content-Type: application/json' \
-d '{
  "requests" : [ {
    "node" : {
      "@id" : "64da3115-8194-44c8-b6f1-cd20b1b7bd67",
      "@type" : "313aa25f-65db-45e9-bb52-74f71ee13ff2"
    }
  }, {
    "node" : {
      "@type" : "3588ea96-6a11-4e76-aa89-3da79a5298e5",
      "value" : "example-api-key-value"
    }
  }, {
    "node" : {
      "@type" : "af7f8403-b442-412a-8891-b0bb6901ed4e",
      "credits_amount" : 1000000000
    }
  }, {
    "link" : {
      "source_node" : 1,
      "target_node" : 0,
      "target_anchor" : "#keys"
    }
  }, {
    "link" : {
      "source_node" : 2,
      "target_node" : 0,
      "target_anchor" : "#credits"
    }
  } ]
}'
```

The important thing about this request:
- `@id` in first node is the username of a new user: `64da3115-8194-44c8-b6f1-cd20b1b7bd67`
- `value` in the second node is the API key: `example-api-key-value`
- `credits_amount` in the third node is the amount of credits that is granted to the user  

After you create a user with the above request example, you can use that user to make requests to SLGL API.
Here's an example of a request to create a new empty node:
```bash
curl <API_URL> \
    -u 64da3115-8194-44c8-b6f1-cd20b1b7bd67:example-api-key-value \
    -H 'Content-Type: application/json' \
    -d '{"requests": [ {"node": {} } ] }'
``` 
