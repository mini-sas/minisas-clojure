import sys
import urllib2


url_base = "http://localhost:8080"

def deref(name):
    url = url_base + "/reference" + name
    response = urllib2.urlopen(url)

    print response.url
    print response.read()

def compare_and_set(name, old_val, new_val):
    url = url_base + "/reference" + name
    headers = {"Content-Type": "message/external-body; access-type=URL; URL=%s" % new_val,
               "If-Match": old_val}

    request = urllib2.Request(url, headers=headers)
    request.get_method = lambda: "PUT"

    response = urllib2.urlopen(request)
    print response.code
    print response.headers
    print response.read()

def set_name(name, new_val):
    url = url_base + "/reference" + name
    headers = {"Content-Type": "message/external-body; access-type=URL; URL=%s" % new_val}

    request = urllib2.Request(url, headers=headers)
    request.get_method = lambda: "PUT"  # really, python? really?

    response = urllib2.urlopen(request)
    print response.read()

def get_value(uuid):
    url = url_base + "/value/" + uuid
    response = urllib2.urlopen(url)

    print response.read()

def save_value(content_type, value):
    url = url_base + "/value"
    headers = {"Content-Type": content_type}
    r = urllib2.Request(url, headers=headers, data=value)
    response = urllib2.urlopen(r)

    print response.url


def main(args):
    command = args[1]

    if command == "deref":
        deref(args[2])

    if command == "get-value":
        get_value(args[2])

    if command == "save-value":
        save_value(args[2], args[3])

    if command == "set":
        set_name(args[2], args[3])

    if command == "compare-and-set":
        compare_and_set(args[2], args[3], args[4])


if __name__ == "__main__":
    main(sys.argv)
