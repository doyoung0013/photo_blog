from rest_framework import serializers
from .models import Post, Comment

class CommentSerializer(serializers.ModelSerializer):
    class Meta:
        model = Comment
        fields = ['id', 'nickname', 'content', 'created_at']


class PostSerializer(serializers.ModelSerializer):
    image = serializers.SerializerMethodField()
    comments = CommentSerializer(many=True, read_only=True)

    def get_image(self, obj):
        # obj가 dict인 경우 (생성 시) 처리
        if isinstance(obj, dict):
            image = obj.get('image')
        else:
            image = obj.image
        
        if image:
            request = self.context.get('request')
            if request:
                return request.build_absolute_uri(image.url)
            return image.url
        return None

    class Meta:
        model = Post
        fields = ['id', 'title', 'text', 'image', 'published_date', 'like_count', 'comments']