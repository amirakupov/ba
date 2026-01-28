data "aws_availability_zones" "available" {}

resource "aws_vpc" "ba_vpc" {
  cidr_block           = "10.10.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "ba-vpc-${var.env}"
  }
}

resource "aws_subnet" "ba_subnet" {
  vpc_id                  = aws_vpc.ba_vpc.id
  cidr_block              = "10.10.1.0/24"
  availability_zone       = data.aws_availability_zones.available.names[0]
  map_public_ip_on_launch = true

  tags = {
    Name = "ba-subnet-${var.env}"
  }
}

resource "aws_internet_gateway" "ba_igw" {
  vpc_id = aws_vpc.ba_vpc.id

  tags = {
    Name = "ba-igw-${var.env}"
  }
}

resource "aws_route_table" "ba_rt" {
  vpc_id = aws_vpc.ba_vpc.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.ba_igw.id
  }

  tags = {
    Name = "ba-rt-${var.env}"
  }
}

resource "aws_route_table_association" "ba_rta" {
  subnet_id      = aws_subnet.ba_subnet.id
  route_table_id = aws_route_table.ba_rt.id
}

resource "aws_security_group" "ba_sg" {
  name        = "ba-sg-${var.env}"
  description = "Allow HTTP 8080 + gRPC 9091 + SSH"
  vpc_id      = aws_vpc.ba_vpc.id

  ingress {
    description = "HTTP 8080"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "gRPC 9091"
    from_port   = 9091
    to_port     = 9091
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  egress {
    description = "All egress"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "ba-sg-${var.env}"
  }
}
